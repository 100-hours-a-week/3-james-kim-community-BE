package ktb.cloud_james.community.controller;

import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.auth.NicknameCheckResponseDto;
import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.dto.user.PasswordUpdateRequestDto;
import ktb.cloud_james.community.dto.user.UserUpdateRequestDto;
import ktb.cloud_james.community.dto.user.UserUpdateResponseDto;
import ktb.cloud_james.community.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자(User) 리소스 관련 API 컨트롤러
 * - 회원가입, 회원정보 수정, 비밀번호 수정 등
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

    /**
     * 닉네임 중복 체크 API (회원정보 수정용)
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNicknameForUpdate(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam String nickname) {

        NicknameCheckResponseDto response = userService.checkNicknameAvailabilityForUpdate(
                currentUserId, nickname
        );

        if (!response.getAvailable()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.success("nickname_already_exists", response));
        }

        return ResponseEntity
                .ok(ApiResponse.success("nickname_available", response));
    }

    /**
     * 회원정보 수정 API (닉네임 + 프로필 이미지)
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<UserUpdateResponseDto>> updateUser(
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody UserUpdateRequestDto request) {

        UserUpdateResponseDto response = userService.updateUser(currentUserId, request);

        return ResponseEntity
                .ok(ApiResponse.success("user_updated", response));
    }

    /**
     * 비밀번호 수정 API
     */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody PasswordUpdateRequestDto request) {

        userService.updatePassword(currentUserId, request);

        return ResponseEntity
                .ok(ApiResponse.success("password_updated", null));
    }
}
