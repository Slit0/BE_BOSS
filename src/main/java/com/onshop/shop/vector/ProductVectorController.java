package com.onshop.shop.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vector")
@RequiredArgsConstructor
@Slf4j

/**
 * ProductVectorController
 * 
 * PostgreSQL 기반의 product_vector 테이블을 관리하는 컨트롤러.
 * 벡터 검색 및 벡터 데이터의 CRUD 기능 제공.
 * 
 * ✅ API 요약:
 * 
 * [GET]    /vector/search?query=검색어         
 *         - 입력 쿼리 기반 벡터 검색 (유사 상품 추천)
 * 
 * [GET]    /vector/{productId}                
 *         - 특정 상품의 벡터 데이터 조회
 * 
 * [POST]   /vector                            
 *         - 벡터 데이터 저장 (벡터 삽입)
 * 
 * [PUT]    /vector/{productId}                
 *         - 벡터 데이터 수정 (벡터 재생성)
 * 
 * [DELETE] /vector/{productId}                
 *         - 벡터 데이터 삭제
 * 
 * [POST]   /vector/sync                       
 *         - MySQL 상품 데이터를 PGVector로 일괄 동기화
 */

public class ProductVectorController {

    private final ProductVectorService productVectorService;
    private final ChatGPTService chatGPTService;
    
    // ✅ 1. 벡터 검색 (유사한 상품 추천)
    @GetMapping("/search")
    public ResponseEntity<List<ProductVectorDTO>> searchVectorProducts(@RequestParam String query) {
        log.info("🔍 벡터 검색 요청: {}", query);
        List<ProductVectorDTO> result = productVectorService.searchVectorProducts(query);
        return ResponseEntity.ok(result);
    }

    // ✅ 2. 특정 상품의 벡터 데이터 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductVectorDTO> getVectorByProductId(@PathVariable Long productId) {
        log.info("📄 벡터 데이터 조회 요청: productId={}", productId);
        ProductVectorDTO vectorData = productVectorService.getVectorByProductId(productId);
        return ResponseEntity.ok(vectorData);
    }

    // ✅ 3. 벡터 데이터 저장 (쓰기)
    @PostMapping
    public ResponseEntity<ProductVectorDTO> saveVectorData(@RequestBody ProductVectorDTO vectorDTO) {
        log.info("📝 벡터 데이터 저장 요청: {}", vectorDTO);
        ProductVectorDTO savedVector = productVectorService.saveVectorData(vectorDTO);
        return ResponseEntity.ok(savedVector);
    }

    // ✅ 4. 벡터 데이터 수정 (업데이트)
    @PutMapping("/{productId}")
    public ResponseEntity<ProductVectorDTO> updateVectorData(
            @PathVariable Long productId,
            @RequestBody ProductVectorDTO vectorDTO) {
        log.info("🔄 벡터 데이터 수정 요청: productId={}, data={}", productId, vectorDTO);
        ProductVectorDTO updatedVector = productVectorService.updateVectorData(productId, vectorDTO);
        return ResponseEntity.ok(updatedVector);
    }

    // ✅ 5. 벡터 데이터 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteVectorData(@PathVariable Long productId) {
        log.info("🗑️ 벡터 데이터 삭제 요청: productId={}", productId);
        productVectorService.deleteVectorData(productId);
        return ResponseEntity.ok("Product vector deleted successfully.");
    }

    // ✅ 6. MySQL → PGVector 벡터 데이터 동기화
    @PostMapping("/sync")
    public ResponseEntity<String> syncProductVectors() {
        log.info("🔄 MySQL → PGVector 데이터 동기화 요청");
        productVectorService.syncProductVectors();
        return ResponseEntity.ok("✅ 전체 상품 벡터 동기화 완료");
    }
 // ✅ 테스트용: 단일 상품 임베딩 → PGVector 저장
    @PostMapping("/test/{productId}") // ← ✅ PostMapping 맞아! (임베딩은 "쓰기"니까 POST)
    public ResponseEntity<String> testEmbedProduct(@PathVariable Long productId) {
        log.info("🧪 단일 상품 임베딩 테스트 시작: productId={}", productId);
        productVectorService.testEmbedProduct(productId);
        return ResponseEntity.ok("✅ 테스트 임베딩 완료: productId=" + productId);
    }
 // ✅ 벡터 기반 유사 상품 추천 (RAG 검색)
 // 예외처리함수
    private boolean isValidStructuredQuery(String rewritten) {
        if (rewritten == null || rewritten.isBlank()) return false;

        String[] tokens = rewritten.trim().split(" ");
        if (tokens.length < 4) return false;

        // 마지막 3개는 반드시 숫자여야 함
        for (int i = tokens.length - 3; i < tokens.length; i++) {
            if (!tokens[i].matches("\\d+")) return false;
        }

        return true;
    }
    // GET http://localhost:5000/vector/rag?query=운동 후 먹을 단백질 제품 100개 추천해줘 예산은 6만원쯤!
    // 사용자별로 20회 정도 대화하고 나면 일정주기 동안 대화 불가능하게 하는 로직 같은게 필요해 보임.
    
    @GetMapping("/rag")
    public ResponseEntity<List<Map<String,Object>>> recommendByRag(@RequestParam String query) {
        log.info("🧠 [RAG] 검색 요청 (자연어): {}", query);

        // 1. 자연어 → 정형 쿼리 변환
        String rewritten = chatGPTService.rewriteToStructuredQuery(query);
        log.info("📝 변환된 쿼리: {}", rewritten);

        // ✅ 1-1. GPT 응답이 비정상적인 경우 (예: 숫자 개수가 부족하거나 너무 짧을 때)
        if (!isValidStructuredQuery(rewritten)) {
            log.warn("⚠️ GPT 응답이 올바른 형식이 아님: {}", rewritten);
            return ResponseEntity
                    .badRequest()
                    .body(List.of()); // 혹은 커스텀 메시지 DTO로 에러 전달 가능
        }

        // 2. RAG로직으로 전달
        List<Long> recommended = productVectorService.recommendProductsByRag(rewritten);
        
     // 3. GPT 후처리
        List<Map<String, Object>> finalResult = chatGPTService.rerankWithGpt(recommended, query);

        // 4. 결과 반환
        return ResponseEntity.ok(finalResult);
    }
}