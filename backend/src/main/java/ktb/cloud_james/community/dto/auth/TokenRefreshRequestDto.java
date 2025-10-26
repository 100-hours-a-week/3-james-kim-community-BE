package ktb.cloud_james.community.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 재발급 요청 DTO
 * - POST /api/auth/refresh 요청
 */
@Getter
@NoArgsConstructor
public class TokenRefreshRequestDto {

    @NotBlank(message = "리프레쉬 토큰이 필요합니다.")
    private String refreshToken;
}
