package ktb.cloud_james.community.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ktb.cloud_james.community.dto.auth.*;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.global.session.SessionManager;
import ktb.cloud_james.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증(Authentication) 관련 비즈니스 로직
 * - 로그인, 로그아웃, 중복 체크
 * - 세션 기반 인증 방식 사용
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionManager sessionManager;

    /**
     * 로그인
     * 1. 이메일로 사용자 조회
     * 2. 비밀번호 검증
     * 3. 계정 활성화 상태 확인
     * 4. 세션 생성 (쿠키 발급)
     * 5. 응답 DTO 생성
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request, HttpServletResponse response) {
        log.info("로그인 시도 - email: {}", request.getEmail());

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 존재하지 않는 이메일: {}", request.getEmail());
                    return new CustomException(ErrorCode.INVALID_CREDENTIALS);
                });

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치: email={}", request.getEmail());
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 계정 활성화 상태 확인
        if (!user.getIsActive()) {
            log.warn("로그인 실패 - 비활성 계정: userId={}, email={}", user.getId(), request.getEmail());
            throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // 4. 세션 생성
        sessionManager.createSession(user.getId(), response);

        log.info("로그인 성공 - userId: {}, email: {}", user.getId(), request.getEmail());

        return new LoginResponseDto(user.getId());
    }

    /**
     * 로그아웃
     * 1. 세션 무효화
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Long userId = sessionManager.getSession(request);
        log.info("로그아웃 시도 - userId: {}", userId);

        // 세션 무효화
        sessionManager.expire(request, response);

        log.info("로그아웃 성공 - userId: {}", userId);
    }


    /**
     * - 회원가입 전 검증이므로 AuthService에 위치
     */
    public EmailCheckResponseDto checkEmailAvailability(String email) {
        log.debug("이메일 중복 체크 - email: {}", email);

        boolean exists = userRepository.existsByEmail(email);

        return new EmailCheckResponseDto(!exists);
    }

    /**
     * - 회원가입 전 검증이므로 AuthService에 위치
     */
    public NicknameCheckResponseDto checkNicknameAvailability(String nickname) {
        log.debug("닉네임 중복 체크 - nickname: {}", nickname);

        boolean exists = userRepository.existsByNickname(nickname);

        return new NicknameCheckResponseDto(!exists);
    }
}
