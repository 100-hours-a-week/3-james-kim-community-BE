package ktb.cloud_james.community.global.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import ktb.cloud_james.community.global.constant.SessionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * 세션 관리 유틸리티
 * - 세션 생성, 조회, 삭제
 */
@Slf4j
public final class SessionUtil {

    private SessionUtil() {
    }

    // 세션에 사용자 ID 저장 (로그인 시)
    public static void createSession(HttpServletRequest request, Long userId) {
        HttpSession session = request.getSession(true); // 세션 없으면 생성
        session.setAttribute(SessionConstants.USER_ID, userId);
        log.info("세션 생성 - sessionId: {}, userId: {}", session.getId(), userId);
    }

    // 세션에서 사용자 ID 조회
    public static Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object userId = session.getAttribute(SessionConstants.USER_ID);
        return userId instanceof Long ? (Long) userId : null;
    }

    // 세션 무효화 (로그아웃 시)
    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("세션 무효화 - sessionId: {}", session.getId());
            session.invalidate();
        }
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return getUserId(request) != null;
    }
}
