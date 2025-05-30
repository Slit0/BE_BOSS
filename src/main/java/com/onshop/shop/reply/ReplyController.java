package com.onshop.shop.reply;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/articles/{articleId}/comments")  // ✅ RESTful API 구조 반영
public class ReplyController {

    private final ReplyService replyService;

    public ReplyController(ReplyService replyService) {
        this.replyService = replyService;
    }

    // ✅ 특정 게시물의 댓글 조회
    @GetMapping
    public ResponseEntity<List<ReplyDTO>> getRepliesByArticleId(@PathVariable Long articleId) {
        List<ReplyDTO> replies = replyService.getRepliesByArticleId(articleId);
        return ResponseEntity.ok(replies);
    }

    // ✅ 댓글 작성
    @PostMapping
    public ResponseEntity<ReplyDTO> createReply(@PathVariable Long articleId, @RequestBody ReplyDTO replyDTO) {
        replyDTO.setArticleId(articleId); // 게시물 ID 설정
        ReplyDTO createdReply = replyService.createReply(replyDTO);
        return ResponseEntity.ok(createdReply);
    }

    // ✅ 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteReply(@PathVariable Long articleId, @PathVariable Long commentId) {
        replyService.deleteReply(commentId);
        return ResponseEntity.noContent().build();
    }
}
