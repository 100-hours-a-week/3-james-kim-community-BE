package ktb.cloud_james.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 닉네임 중복 체크 응답 DTO
 * - GET /api/auth/check-nickname 응답
 */
@Getter
@AllArgsConstructor
public class NicknameCheckResponseDto {

    private Boolean available;
}
