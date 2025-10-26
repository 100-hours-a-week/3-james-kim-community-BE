package ktb.cloud_james.community.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 수정 요청 DTO
 * - 새 비밀번호 + 새 비밀번호 확인
 * - 프론트에서 1차 검증, 서버에서 2차 검증 (이중 검증)
 */
@Getter
@NoArgsConstructor
public class PasswordUpdateRequestDto {

    /**
     * 새 비밀번호
     * - 8~20자, 대소문자, 숫자, 특수문자 각 1개 이상
     */
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    )
    private String newPassword;

    /**
     * 새 비밀번호 확인
     * - newPassword와 동일해야 함
     * - 프론트에서 포커스 아웃 시 실시간 검증
     */
    @NotBlank(message = "새 비밀번호를 한번 더 입력해주세요.")
    private String newPasswordConfirm;
}