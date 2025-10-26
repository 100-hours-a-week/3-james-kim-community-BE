package ktb.cloud_james.community.dto.like;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 좋아요 응답 DTO
 * - 좋아요 추가/취소 성공 시 클라이언트에게 반환
 * - 현재 좋아요 상태 + 업데이트된 좋아요 수 포함
 */
@Getter
@AllArgsConstructor
public class LikeResponseDto {

    private Boolean isLiked;

    private Long likeCount;
}
