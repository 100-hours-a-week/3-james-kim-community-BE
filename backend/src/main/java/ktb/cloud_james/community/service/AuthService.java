package ktb.cloud_james.community.service;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import ktb.cloud_james.community.dto.auth.*;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.entity.UserToken;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.global.security.JwtTokenProvider;
import ktb.cloud_james.community.global.util.CookieUtil;
import ktb.cloud_james.community.global.util.TokenUtil;
import ktb.cloud_james.community.repository.UserRepository;
import ktb.cloud_james.community.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 인증(Authentication) 관련 비즈니스 로직
 * - 이메일/닉네임 중복 체크 (회원가입 전 검증)
 * - 토큰 갱신
 * - 로그인/로그아웃
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;

    /**
     * 로그인
     * 1. 이메일로 사용자 조회
     * 2. 탈퇴/비활성 회원 차단 (회원탈퇴 후 추가된 로직)
     * 3. 비밀번호 검증
     * 4. 토큰 발급
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

        // 2. 탈퇴/비활성 회원 차단 (회원탈퇴 후 추가된 로직)
        if (user.isDeleted() || !user.getIsActive()) {
            log.warn("로그인 실패 - 탈퇴 또는 비활성 회원: email={}", request.getEmail());
            throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치: email={}", request.getEmail());
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 5. Refresh Token DB 저장 (기존 토큰 있으면 갱신)
        tokenUtil.saveRefreshToken(user, refreshToken);

        // 6. addRefreshToken 쿠키 설정
        CookieUtil.addRefreshTokenCookie(response, refreshToken);

        log.info("로그인 성공 - userId: {}", user.getId());

        return new LoginResponseDto(accessToken);
    }

    /**
     * 로그아웃
     * 1. 현재 로그인한 사용자 조회
     * 2. DB에서 해당 사용자의 Refresh Token 삭제
     */
    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        log.info("로그아웃 시도 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("로그아웃 실패 - 존재하지 않는 사용자: {}", userId);
                    return new CustomException(ErrorCode.UNAUTHORIZED);
                });

        // 2. DB에서 Refresh Token 삭제
        userTokenRepository.findByUser(user)
                .ifPresent(userToken -> {
                    userTokenRepository.delete(userToken);
                    log.info("Refresh Token 삭제 완료 - userId: {}", userId);
                });

        // RefreshToken 쿠키 제거
        CookieUtil.deleteRefreshTokenCookie(response);

        log.info("로그아웃 성공 - userId: {}", userId);
    }


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

    /**
     *
     */
    @Transactional
    public TokenDto refreshAccessToken(String refreshToken) {
        log.info("토큰 갱신 시도");

        // 1. Refresh Token JWT 자체 유효성 검증 (서명, 만료시간)
        Long userId;
        try {
            // 2. JWT에서 userId 추출 (만료되어도 추출 가능)
            userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        } catch (ExpiredJwtException e) {
            // JWT가 만료되어도 Claims에서 userId 추출 가능
            userId = Long.parseLong(e.getClaims().getSubject());
            log.info("만료된 JWT에서 userId 추출: {}", userId);
        } catch (Exception e) {
            log.warn("토큰 갱신 실패 - 유효하지 않은 JWT");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 3. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 - 존재하지 않는 사용자");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        // 4. 탈퇴/비활성 회원 차단
        if (user.isDeleted() || !user.getIsActive()) {
            log.warn("토큰 갱신 실패 - 탈퇴 또는 비활성 회원: userId={}, isDeleted={}, isActive={}",
                    userId, user.isDeleted(), user.getIsActive());

            // 탈퇴 회원의 토큰은 즉시 삭제
            userTokenRepository.findByUser(user).ifPresent(userTokenRepository::delete);

            throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // 5. DB에서 해당 User의 Refresh Token 조회
        UserToken userToken = userTokenRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 - DB에 토큰 없음");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        // 6. 클라이언트가 보낸 Refresh Token과 DB의 암호화된 토큰 비교
        String hashedRefreshToken = tokenUtil.hashRefreshToken(refreshToken);
        if (!hashedRefreshToken.equals(userToken.getRefreshToken())) {
            log.warn("토큰 갱신 실패 - 토큰 불일치: userId={}", userId);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 7. 만료 시간 확인
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("토큰 갱신 실패 - 만료된 토큰: userId={}", userId);
            // 만료된 토큰 삭제
            userTokenRepository.delete(userToken);
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 8. 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());

        log.info("토큰 갱신 성공 - userId: {}", userId);

        // Refresh Token은 그대로 사용
        return new TokenDto(newAccessToken);
    }

}