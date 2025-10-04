package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.auth.EmailCheckResponseDto;
import ktb.cloud_james.community.dto.auth.NicknameCheckResponseDto;
import ktb.cloud_james.community.dto.auth.TokenDto;
import ktb.cloud_james.community.entity.UserToken;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.global.security.JwtTokenProvider;
import ktb.cloud_james.community.repository.UserRepository;
import ktb.cloud_james.community.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증(Authentication) 관련 비즈니스 로직
 * - 이메일/닉네임 중복 체크 (회원가입 전 검증)
 * - 토큰 갱신
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * - 회원가입 전 검증이므로 AuthService에 위치
     */
    public EmailCheckResponseDto checkEmailAvailability(String email) {
        boolean exists = userRepository.existsByEmail(email);

        return new EmailCheckResponseDto(!exists);
    }

    /**
     * - 회원가입 전 검증이므로 AuthService에 위치
     */
    public NicknameCheckResponseDto checkNicknameAvailability(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);

        return new NicknameCheckResponseDto(!exists);
    }

    @Transactional
    public TokenDto refreshAccessToken(String refreshToken) {
        log.info("토큰 갱신 시도");

        // 1. Refresh Token JWT 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("토큰 갱신 실패 - 유효하지 않은 토큰");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. DB에서 Refresh Token 조회
        UserToken userToken = userTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 - DB에 없는 토큰");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        // 3. 만료 시간 확인
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("토큰 갱신 실패 - 만료된 토큰");
            // 만료된 토큰 삭제
            userTokenRepository.delete(userToken);
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 4. 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 5. 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        log.info("토큰 갱신 성공 - userId: {}", userId);

        return new TokenDto(newAccessToken, refreshToken);
    }
}
