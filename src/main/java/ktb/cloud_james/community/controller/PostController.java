package ktb.cloud_james.community.controller;

import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.dto.post.PostCreateRequestDto;
import ktb.cloud_james.community.dto.post.PostCreateResponseDto;
import ktb.cloud_james.community.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 API 컨트롤러
 * - 게시글 CRUD 처리
 */
@RestController
@RequestMapping("/api/posts")
@Slf4j
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성 API 사용법:
     * Headers: Authorization: Bearer {access_token}
     * Body: {
     *  "title": "게시글 제목",
     *  "content": "게시글 내용",
     *  "imageUrl": "/temp/abc-123.jpg"  // 선택
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponseDto>> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostCreateRequestDto request) {

        PostCreateResponseDto response = postService.createPost(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("post_created", response));
    }
}
