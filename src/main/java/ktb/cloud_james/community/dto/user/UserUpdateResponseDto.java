package ktb.cloud_james.community.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원정보 수정 응답 DTO
 */
@Getter
@AllArgsConstructor
public class UserUpdateResponseDto {

    private Long userId;
}