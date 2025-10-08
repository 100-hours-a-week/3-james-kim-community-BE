package ktb.cloud_james.community.dto.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 상세 조회 응답 DTO
 * - 게시글 전체 정보 + 작성자 + 통계
 * - 댓글 목록은 제외 (페이징 필요하기 때문에 따로 API 분리)
 */
@Getter
@Builder
@AllArgsConstructor
public class PostDetailResponseDto {

    private Long postId;

    private String title;

    private String content;

    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // 작성자인 경우에 필요하다면 활용

    private AuthorInfo author;

    private StatsInfo stats;

    private Boolean isLiked;   // 좋아요 여부
    private Boolean isAuthor;  // 작성자 여부 (수정/삭제 권한)

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AuthorInfo {
        private String nickname;
        private String profileImage;
    }

    /**
     * 통계 정보
     * - viewsCount는 이미 +1 반영된 값
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StatsInfo {
        private Long likeCount;
        private Long commentCount;
        private Long viewCount;  // 조회 시 자동 증가된 값
    }
}
