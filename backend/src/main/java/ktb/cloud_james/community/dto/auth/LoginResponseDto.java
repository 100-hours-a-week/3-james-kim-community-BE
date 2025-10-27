package ktb.cloud_james.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 * - 세션 기반 인증이므로 토큰 없이 userId 반환
 */
@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private Long userId;
}
