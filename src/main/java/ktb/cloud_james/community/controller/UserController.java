package ktb.cloud_james.community.controller;

import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자(User) 리소스 관련 API 컨트롤러
 * - 회원가입 등
 *
 * @RestController: @Controller + @ResponseBody
 * @RequestMapping: 기본 URL 경로 /api/users
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 API
     * @Valid: DTO의 유효성 검증 자동 실행
     * - @NotBlank, @Email, @Pattern 등 검증
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SignUpResponseDto>> signUp(
            @Valid @RequestBody SignUpRequestDto request) {

        SignUpResponseDto response = userService.signUp(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("signup_success", response));
    }

}
