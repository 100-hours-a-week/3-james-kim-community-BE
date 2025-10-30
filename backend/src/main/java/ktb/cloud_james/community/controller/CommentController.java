package ktb.cloud_james.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.comment.*;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 댓글 API 컨트롤러
 * - 댓글 CRUD 처리
 */
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@Slf4j
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성 API
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentCreateResponseDto>> createComment(
            @PathVariable Long postId,
            HttpServletRequest httpRequest,
            @Valid @RequestBody CommentCreateRequestDto request
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        CommentCreateResponseDto response = commentService.createComment(userId, postId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("comment_created", response));
    }

    /**
     * 댓글 목록 조회 API (인피니티 스크롤)
     * - 첫 페이지: GET /api/posts/1/comments?limit=10
     * - 다음 페이지: GET /api/posts/1/comments?lastSeenId=11&limit=10
     * Headers: Authorization: Bearer {access_token}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CommentListResponseDto>> getCommentList(
            @PathVariable Long postId,
            @RequestParam(required = false) Long lastSeenId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        CommentListResponseDto response = commentService.getCommentList(postId, lastSeenId, limit, userId);

        return ResponseEntity
                .ok(ApiResponse.success("comments_retrieved", response));
    }


    /**
     * 댓글 수정 API
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentUpdateResponseDto>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            HttpServletRequest httpRequest,
            @Valid @RequestBody CommentUpdateRequestDto request
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        CommentUpdateResponseDto response = commentService.updateComment(userId, postId, commentId, request);

        return ResponseEntity
                .ok(ApiResponse.success("comment_updated", response));
    }

    /**
     * 댓글 삭제 API
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDeleteResponseDto>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        CommentDeleteResponseDto response = commentService.deleteComment(userId, postId, commentId);

        return ResponseEntity
                .ok(ApiResponse.success("comment_deleted", response));
    }
}
