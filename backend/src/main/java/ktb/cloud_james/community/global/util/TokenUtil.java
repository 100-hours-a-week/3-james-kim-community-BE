package ktb.cloud_james.community.global.util;

import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.entity.UserToken;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.global.security.JwtTokenProvider;
import ktb.cloud_james.community.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 토큰 관리 유틸리티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUtil {

    private final UserTokenRepository userTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token DB 저장
     */
    public void saveRefreshToken(User user, String refreshToken) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenValidity() / 1000);

        userTokenRepository.findByUser(user)
                .ifPresent(userTokenRepository::delete);

        String hashedRefreshToken = hashRefreshToken(refreshToken);

        UserToken userToken = UserToken.builder()
                .user(user)
                .refreshToken(hashedRefreshToken)
                .expiresAt(expiresAt)
                .build();

        userTokenRepository.save(userToken);
        log.debug("Refresh Token 저장 완료 - userId: {}", user.getId());
    }

    /**
     * Refresh Token SHA-256 해싱
     */
    public String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 알고리즘을 찾을 수 없음", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}