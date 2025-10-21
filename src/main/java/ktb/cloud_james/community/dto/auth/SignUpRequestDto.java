package ktb.cloud_james.community.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 * - 클라이언트로부터 받는 회원가입 데이터
 * - Bean Validation으로 유효성 검사
 */
@Getter
@NoArgsConstructor // Jackson이 JSON을 객체로 변환할 때 필요
public class SignUpRequestDto {

    /**
     * @NotBlank: null, 빈 문자열, 공백만 있는 문자열 모두 불허
     * @Email: 이메일 형식 검증
     */
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 주소 형식을 입력해주세요. (예: example@example.com)")
    private String email;

    /**
     * @Pattern: 정규표현식으로 검증
     * - (?=.*[a-z])                 소문자 최소 1개
     * - (?=.*[A-Z])                 대문자 최소 1개
     * - (?=.*\\d)                   숫자 최소 1개
     * - (?=.*[@$!%*?&])             특수문자 최소 1개
     * - [A-Za-z\\d@$!%*?&]{8,20}    허용된 문자로만 8~20자
     */
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    )
    private String password;

    /**
     * - password와 동일해야 함 (Service 레이어에서 검증)
     */
    @NotBlank(message = "비밀번호를 한번 더 입력해주세요.")
    private String passwordConfirm;

    /**
     * @Size: 길이 제한
     * @Pattern: 띄어쓰기 검증
     */
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 10, message = "닉네임은 최대 10자까지 작성 가능합니다.")
    @Pattern(regexp = "^\\S+$", message = "띄어쓰기는 불가능합니다.")
    private String nickname;

    private String profileImage;

    @Builder
    public SignUpRequestDto(String email, String password, String passwordConfirm, String nickname, String profileImage) {
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }
}
