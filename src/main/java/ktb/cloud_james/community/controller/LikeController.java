package ktb.cloud_james.community.controller;

import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.dto.like.LikeResponseDto;
import ktb.cloud_james.community.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좋아요 API 컨트롤러
 * - 좋아요 추가/취소
 */
@RestController
@RequestMapping("/api/posts/{postId}/like")
@Slf4j
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 토글 API 동작:
     * - 좋아요 안 눌려있음 → 추가 (INSERT, isLiked: true, likeCount +1)
     * - 이미 좋아요 누름 → 취소 (DELETE, isLiked: false, likeCount -1)
     * - 클라이언트는 무조건 POST만 날리면 됨 (서버가 알아서 판단)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponseDto>> like(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {

        LikeResponseDto response = likeService.like(userId, postId);

        return ResponseEntity
                .ok(ApiResponse.success("like_updated", response));
    }
}
