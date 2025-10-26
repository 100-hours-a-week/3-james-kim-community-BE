package ktb.cloud_james.community.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 비밀번호 수정 응답 DTO
 */
@Getter
@AllArgsConstructor
public class PasswordUpdateResponseDto {

   private Long userId;
}