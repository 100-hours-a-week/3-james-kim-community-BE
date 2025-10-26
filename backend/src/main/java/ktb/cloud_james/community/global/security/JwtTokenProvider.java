package ktb.cloud_james.community.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증
 * - Access Token 생성
 * - Refresh Token 생성
 * - 토큰 검증 및 사용자 ID 추출
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    /**
     * 생성자 - application.yml의 JWT 설정값 주입
     * @param secret JWT 서명용 비밀키
     * @param accessTokenValidity Access Token 유효시간 (밀리초)
     * @param refreshTokenValidity Refresh Token 유효시간 (밀리초)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        // HS256 알고리즘용 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * JWT는 3개 부분으로 구성
     * Header.Payload,Signature
     */
    public String createAccessToken(Long userId) {
        // JJWT는 Date만 지원. LocalDateTime을 지원하지 않는다.
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidity);

        /**
         * subject = JWT의 주인공: payload에 들어가는 핵심 데이터
         * 결과 Payload 예시:
         * {
         *   "sub": "123",
         *   "iat": 1704297600,
         *   "exp": 1704301200
         * }
         */
        return Jwts.builder()
                .subject(String.valueOf(userId)) // Payload: 사용자 Id
                .issuedAt(now)                   // 발급 시간
                .expiration(validity)            // 만료 시간
                .signWith(secretKey)             // 서명 -> signature 생성
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // Payload: 사용자 Id
                .issuedAt(now)                   // 발급 시간
                .expiration(validity)            // 만료 시간
                .signWith(secretKey)             // 서명 -> signature 생성
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        /**
         * .parseSignedClaims(token)
         * 서명된 JWT 토큰을 파싱 -> 토큰을 분해하고 서명 검증
         *
         * 내부 동작:
         * 1. 토큰을 Header, Payload, Signature로 분리
         * 2. Signature 검증 (verifyWith의 secretKey 사용)
         * 3. 만료 시간 확인
         * 4. 모두 통과하면 Claims 반환
         */

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) throws ExpiredJwtException {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT 서명 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있음: {}", e.getMessage());
        }

        return false;
    }

    public long getRefreshTokenValidity() {
        return this.refreshTokenValidity;
    }
}
