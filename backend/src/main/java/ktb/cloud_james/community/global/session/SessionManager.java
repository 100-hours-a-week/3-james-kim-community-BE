package ktb.cloud_james.community.global.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import ktb.cloud_james.community.global.constant.SessionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세션 관리 클래스
 * - 세션 생성, 조회, 만료
 */
@Slf4j
@Component
public class SessionManager {

    // 동시성 고려 (Key-Value 인메모리 저장 방식 - 확장한다면 Redis 메모리DB 같은)
    private final Map<String, Long> sessionStore = new ConcurrentHashMap<>();

    /**
     * 세션 생성
     * 1. UUID로 세션 ID 생성 (추적 불가능)
     * 2. 세션 저장소에는 sessionId와 식별자 userId 저장
     * 3. 쿠키에 sessionId 담아서 전달
     */
    public void createSession(Long userId, HttpServletResponse response) {
        // 1. 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();

        // 2. 세션 저장소에 저장
        sessionStore.put(sessionId, userId);

        log.info("세션 생성 - sessionId: {}, userId: {}", sessionId, userId);

        // 3. 쿠키 생성 및 응답에 추가
        Cookie sessionCookie = new Cookie(SessionConstants.SESSION_COOKIE_NAME, sessionId);
        sessionCookie.setHttpOnly(true); // JS 접근 차단
        sessionCookie.setPath("/");      // 경로 설정
        sessionCookie.setMaxAge(3600);   // 만료 시간 정의

        response.addCookie(sessionCookie);
    }

    /**
     * 세션 조회
     * 1. 쿠키에서 세션 ID 추출
     * 2. 세션 저장소에서 userId 조회
     */
    public Long getSession(HttpServletRequest request) {
        Cookie sessionCookie = findCookie(request, SessionConstants.SESSION_COOKIE_NAME);

        if (sessionCookie == null) {
            return null;
        }

        String sessionId = sessionCookie.getValue();
        Long userId = sessionStore.get(sessionId);

        log.debug("세션 조회 - sessionId: {}, userId: {}", sessionId, userId);

        return userId;
    }

    /**
     *  세션 만료
     *  1. 쿠키에서 세션 ID 추출
     *  2. 세션 저장소에서 제거
     *  3. 쿠키 무효화
     */
    public void expire(HttpServletRequest request, HttpServletResponse response) {
        Cookie sessionCookie = findCookie(request, SessionConstants.SESSION_COOKIE_NAME);

        if (sessionCookie != null) {
            String sessionId = sessionCookie.getValue();

            sessionStore.remove(sessionId);

            log.info("세션 만료 - sessionId: {}", sessionId);

            // 쿠키 무효화
            Cookie expireCookie = new Cookie(SessionConstants.SESSION_COOKIE_NAME, null);
            expireCookie.setPath("/");
            expireCookie.setMaxAge(0);
            response.addCookie(expireCookie);
        }
    }

    // ===== 헬퍼 메서드 =====
    private Cookie findCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie ->  cookie.getName().equals(cookieName))
                .findAny()
                .orElse(null);
    }

}
