package ktb.cloud_james.community.dto.comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 목록 조회 응답 DTO
 * - 인피니티 스크롤용 댓글 리스트
 * - 게시글 상세 페이지에서 사용
 */
@Getter
@Builder
@AllArgsConstructor
public class CommentListResponseDto {

    // 댓글 목록
    private List<CommentSummaryDto> comments;

    // 페이징 정보
    private PaginationInfo pagination;

    @Getter
    @AllArgsConstructor
    public static class CommentSummaryDto {
        private Long commentId;
        private String content;
        private String authorNickname;
        private String authorProfileImage;

        @JsonIgnore
        private Boolean isAuthorDeleted;     // 탈퇴 여부 (내부 처리용)

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private Boolean isAuthor;

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

        private Long lastSeenId; // 다음 페이지 요청 시 사용할 커서 (마지막 댓글 ID)
        private Boolean hasNext; // 다음 페이지 존재 여부
        private Integer limit;   // 페이지당 댓글 수
    }
}
