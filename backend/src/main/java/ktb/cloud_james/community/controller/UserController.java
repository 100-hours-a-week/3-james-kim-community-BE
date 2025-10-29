package ktb.cloud_james.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ktb.cloud_james.community.dto.auth.NicknameCheckResponseDto;
import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.dto.user.*;
import ktb.cloud_james.community.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자(User) 리소스 관련 API 컨트롤러
 * - 회원가입, 회원정보 수정, 비밀번호 수정, 회원탈퇴 등
 * - 세션 기반 인증 사용
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
            @Valid @RequestBody SignUpRequestDto request,
            HttpServletRequest httpRequest) {

        SignUpResponseDto response = userService.signUp(request, httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("signup_success", response));
    }

    /**
     * 닉네임 중복 체크 API (회원정보 수정용)
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNicknameForUpdate(
            @RequestAttribute("userId") Long userId,
            @RequestParam String nickname) {

        NicknameCheckResponseDto response = userService.checkNicknameAvailabilityForUpdate(
                userId, nickname
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
     * 회원정보 조회 API (이메일, 닉네임, 프로필 이미지)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> getUserInfo(
            @RequestAttribute("userId") Long userId
    ) {

        UserInfoResponseDto response = userService.getUserInfo(userId);

        return ResponseEntity
                .ok(ApiResponse.success("user_info_retrieved", response));
    }

    /**
     * 회원정보 수정 API (닉네임 + 프로필 이미지)
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<UserUpdateResponseDto>> updateUser(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UserUpdateRequestDto request) {

        UserUpdateResponseDto response = userService.updateUser(userId, request);

        return ResponseEntity
                .ok(ApiResponse.success("user_updated", response));
    }

    /**
     * 비밀번호 수정 API
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<PasswordUpdateResponseDto>> updatePassword(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PasswordUpdateRequestDto request
    ) {

        PasswordUpdateResponseDto response = userService.updatePassword(userId, request);

        return ResponseEntity
                .ok(ApiResponse.success("password_updated", response));
    }

    /**
     * 회원탈퇴 API
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdrawUser(
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        userService.withdrawUser(userId, httpRequest);

        return ResponseEntity
                .ok(ApiResponse.success("account_deleted", null));
    }
}
