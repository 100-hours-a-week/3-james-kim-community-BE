package ktb.cloud_james.community.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠키 관리 유틸리티
 * - RefreshToken 쿠키 생성/삭제만 담당
 */
@Slf4j
public final class CookieUtil {

    private CookieUtil() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * RefreshToken 쿠키 생성
     */
    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 운영 환경에서는 true로 설정
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(cookie);

        log.debug("RefreshToken 쿠키 생성 완료");
    }

    /**
     * RefreshToken 쿠키 삭제
     */
    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        log.debug("RefreshToken 쿠키 삭제 완료");
    }
}