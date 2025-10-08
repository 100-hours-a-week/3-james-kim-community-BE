package ktb.cloud_james.community.controller;

import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.dto.post.PostCreateRequestDto;
import ktb.cloud_james.community.dto.post.PostCreateResponseDto;
import ktb.cloud_james.community.dto.post.PostListResponseDto;
import ktb.cloud_james.community.service.PostService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 게시글 목록 조회 API (인피니티 스크롤) 사용법:
     * - 첫 페이지: GET /api/posts?limit=20
     * - 다음 페이지: GET /api/posts?lastSeenId=15&limit=2
     * Headers: (일반적인 SNS는 비로그인도 볼 수 있지만, 이 커뮤니티는 기획상 로그인해야만 확인이 가능하다. -> 확장은 쉽게 가능)
     * - Authorization: Bearer {access_token} (필수)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PostListResponseDto>> getPostList(
            @RequestParam(required = false) Long lastSeenId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal Long userId
    ) {
        PostListResponseDto response = postService.getPostList(lastSeenId, limit, userId);

        return ResponseEntity
                .ok(ApiResponse.success("posts_retrieved", response));
    }
}
