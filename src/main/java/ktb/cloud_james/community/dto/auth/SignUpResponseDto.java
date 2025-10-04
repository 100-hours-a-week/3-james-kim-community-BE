package ktb.cloud_james.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원가입 응답 DTO
 * - 회원가입 성공 시 클라이언트에게 반환
 * - 생성된 사용자 ID만 포함
 */
@Getter
@AllArgsConstructor
public class SignUpResponseDto {

    private Long userId;
}
