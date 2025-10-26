package ktb.cloud_james.community.dto.comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 댓글 작성 응답 DTO
 * - 댓글 작성 성공 시 클라이언트에게 반환
 * - 생성된 댓글 정보 + 게시글 통계 업데이트 정보
 */
@Getter
@Builder
@AllArgsConstructor
public class CommentCreateResponseDto {

    // 생성된 댓글 정보
    private CommentInfo comment;

    // 게시글 통계 정보 (댓글 수 증가 반영)
    private Long commentsCount;

    /**
     * 댓글 상세 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CommentInfo {
        private Long commentId;
        private String content;
        private String authorNickname;
        private String authorProfileImage;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
