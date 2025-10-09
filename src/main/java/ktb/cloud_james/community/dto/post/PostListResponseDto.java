package ktb.cloud_james.community.dto.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 목록 조회 응답 DTO
 * - 인피니티 스크롤용 게시글 리스트
 */
@Getter
@Builder
@AllArgsConstructor
public class PostListResponseDto {

    // 게시글 목록
    private List<PostSummaryDto> posts;

    // 페이징 정보
    private PaginationInfo pagination;

    // 개별 게시글 요약 정보 - 화면에 보여지는 정보들
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PostSummaryDto {
        private Long postId;
        private String title;                // 제목
        private String authorNickname;       // 작성자
        private String authorProfileImage;   // 작성자 프로필 이미지

        @JsonIgnore
        private Boolean isAuthorDeleted;     // 탈퇴 여부 (내부 처리용)

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;      // 게시글 작성일시

        private Long likeCount;
        private Long commentCount;
        private Long viewCount;

        private Boolean isLiked;

        // 탈퇴한 회원이면 닉네임/이미지 변경
        public void maskDeletedUser() {
            if (isAuthorDeleted != null && isAuthorDeleted) {
                this.authorNickname = "탈퇴한 회원";
                this.authorProfileImage = null;
            }
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PaginationInfo {
        private Long lastSeenId; // 다음 페이지 요청 시 사용할 ID
        private Boolean hasNext; // 다음 페이지 존재 여부
        private Integer limit;   // 페이지당 게시글 수
        private String sort;     // 정렬 기준 (latest)
    }
}
