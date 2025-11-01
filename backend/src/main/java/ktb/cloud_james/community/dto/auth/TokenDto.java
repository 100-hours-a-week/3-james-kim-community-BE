package ktb.cloud_james.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 DTO
 * - Service 레이어에서 생성
 * - Access Token + Refresh Token 함께 관리
 */
@Getter
@AllArgsConstructor
public class TokenDto {
    private String accessToken;
}
