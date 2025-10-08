package ktb.cloud_james.community.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 작성 요청 DTO
 * - 클라이언트로부터 받는 댓글 작성 데이터
 */
@Getter
@NoArgsConstructor
public class CommentCreateRequestDto {

    /**
     * 댓글 내용
     * - TEXT 타입이므로 길이 제한 없음
     * - 빈 문자열/공백만 있는 경우 불가
     */
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;
}
