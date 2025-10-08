package ktb.cloud_james.community.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 댓글 삭제 응답 DTO
 * - 삭제된 게시글의 최신 댓글 수 포함
 */
@Getter
@AllArgsConstructor
public class CommentDeleteResponseDto {

    private Long commentsCount;
}
