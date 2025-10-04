package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.auth.EmailCheckResponseDto;
import ktb.cloud_james.community.dto.auth.NicknameCheckResponseDto;
import ktb.cloud_james.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증(Authentication) 관련 비즈니스 로직
 * - 이메일/닉네임 중복 체크 (회원가입 전 검증)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    /**
     * 이메일 중복 체크
     * - 회원가입 시 포커스 아웃 시 자동 호출
     * - 회원가입 전 검증이므로 AuthService에 위치
     */
    public EmailCheckResponseDto checkEmailAvailability(String email) {
        boolean exists = userRepository.existsByEmail(email);

        return new EmailCheckResponseDto(!exists);
    }

    /**
     * 닉네임 중복 체크
     * - 회원가입 시 포커스 아웃 시 자동 호출
     * - 회원가입 전 검증이므로 AuthService에 위치
     */
    public NicknameCheckResponseDto checkNicknameAvailability(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);

        return new NicknameCheckResponseDto(!exists);
    }
}
