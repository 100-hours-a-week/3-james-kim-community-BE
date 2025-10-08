package ktb.cloud_james.community.controller;

import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.comment.CommentCreateRequestDto;
import ktb.cloud_james.community.dto.comment.CommentCreateResponseDto;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<ApiResponse<CommentCreateResponseDto>> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentCreateRequestDto request
    ) {

        CommentCreateResponseDto response = commentService.createComment(userId, postId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("comment_created", response));
    }
}
