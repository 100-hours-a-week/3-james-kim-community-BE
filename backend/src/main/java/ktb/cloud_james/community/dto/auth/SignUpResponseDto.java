package ktb.cloud_james.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원가입 응답 DTO (토큰 포함으로 수정)
 * - 회원가입 성공 시 클라이언트에게 반환
 * - 생성된 사용자 ID + Access Token
 * - Refresh Token은 별도로 클라이언트가 저장 필요
 */
@Getter
@AllArgsConstructor
public class SignUpResponseDto {

    private Long userId;

    private String accessToken;

    private String refreshToken;
}
