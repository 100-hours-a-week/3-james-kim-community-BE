package ktb.cloud_james.community.global.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import ktb.cloud_james.community.global.constant.SessionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * 세션 관리 유틸리티 클래스
 * - 세션 생성, 조회, 만료
 */
@Slf4j
public final class SessionUtil {

    private SessionUtil() {
    }

    /**
     * 세션 생성 (HttpSession : 대부분의 과정을 자동화)
     * - UUID 생성, 세션 저장소 (톰캣 메모리)
     * - 쿠키 생성, 세션 타임 아웃 설정
     * - 세션 저장소: 중첩 맵 형태로 이해 -> key: 상수로 고정
     */
    public static void createSession(HttpServletRequest request, Long userId) {
        HttpSession session = request.getSession(true); // 세션 없으면 생성
        session.setAttribute(SessionConstants.USER_ID, userId);
        log.info("세션 생성 - sessionId: {}, userId: {}", session.getId(), userId);
    }

    // 세션에서 사용자 ID 조회
    public static Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // 세션 없으면 null
        if (session == null) {
            return null;
        }

        Object userId = session.getAttribute(SessionConstants.USER_ID);
        return userId instanceof Long ? (Long)userId : null;
    }

    /**
     * 세션 무효화(로그아웃 시): invalidate()
     * - 세션 저장소에서 세션 제거
     * - 세션 속성 전부 언바인딩(unbind)
     * - 세션 쿠키 정리
     */
    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // 세션 없으면 null
        if (session != null) {
            log.info("세션 무효화 - sessionId: {}", session.getId());
            session.invalidate();
        }
    }

    // 현재 사용자가 로그인 상태인지 확인
    public static boolean isAuthenticated(HttpServletRequest request) {
        return getUserId(request) != null;
    }
}
