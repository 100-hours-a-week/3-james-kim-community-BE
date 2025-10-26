package ktb.cloud_james.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 이메일 중복 체크 응답 DTO
 * - GET /api/auth/check-email 응답
 */
@Getter
@AllArgsConstructor
public class EmailCheckResponseDto {

    private Boolean available;
}
