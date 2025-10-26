package ktb.cloud_james.community.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 댓글 수정 응답 DTO
 * - 댓글 수정 성공 시 클라이언트에게 반환
 * - 수정된 댓글 ID만 포함
 */
@Getter
@AllArgsConstructor
public class CommentUpdateResponseDto {

    private Long commentId;
}
