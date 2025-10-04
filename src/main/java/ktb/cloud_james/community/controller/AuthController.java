package ktb.cloud_james.community.controller;

import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.auth.EmailCheckResponseDto;
import ktb.cloud_james.community.dto.auth.NicknameCheckResponseDto;
import ktb.cloud_james.community.dto.auth.TokenDto;
import ktb.cloud_james.community.dto.auth.TokenRefreshRequestDto;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증(Authentication) 관련 API 컨트롤러
 * - 중복 체크, 토큰 갱신 등
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 이메일 중복 체크 API
     * @RequestParam: 쿼리 파라미터 바인딩
     * - URL의 ?email=xxx 값을 매개변수로 받음
     * - 이메일은 그렇게 민감한 중요 정보라고 판단하지 않아 파라미터로 받기로 결정
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailCheckResponseDto>> checkEmail(
            @RequestParam String email) {

        EmailCheckResponseDto response = authService.checkEmailAvailability(email);

        // 이메일 이미 존재하면 409 Conflict 반환
        if (!response.getAvailable()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.success("email_already_exists", response));
        }

        return ResponseEntity
                .ok(ApiResponse.success("email_available", response));
    }

    /**
     * 닉네임 중복 체크 API
     * @RequestParam: 쿼리 파라미터 바인딩
      * - URL의 ?nickname=xxx 값을 매개변수로 받음
      * - 닉네임 역시 그렇게 민감한 중요 정보라고 판단하지 않아 파라미터로 받기로 결정
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNickname(
            @RequestParam String nickname) {

        NicknameCheckResponseDto response = authService.checkNicknameAvailability(nickname);

        // 닉네임이 이미 존재하면 409 Conflict 반환
        if (!response.getAvailable()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.success("nickname_already_exists", response));
        }

        return ResponseEntity
                .ok(ApiResponse.success("nickname_available", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(
            @Valid @RequestBody TokenRefreshRequestDto request) {

        TokenDto tokens = authService.refreshAccessToken(request.getRefreshToken());

        return ResponseEntity
                .ok(ApiResponse.success("token_refreshed", tokens));
    }
}
