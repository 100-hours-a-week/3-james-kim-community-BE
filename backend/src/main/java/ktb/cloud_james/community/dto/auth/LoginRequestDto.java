package ktb.cloud_james.community.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 주소 형식을 입력해주세요. (예: example@example.com)")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
