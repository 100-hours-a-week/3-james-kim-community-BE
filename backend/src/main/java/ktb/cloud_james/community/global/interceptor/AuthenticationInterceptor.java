package ktb.cloud_james.community.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ktb.cloud_james.community.global.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인증 인터셉터
 * - 로그인 필요한 API에 대한 인증 확인
 * - 세션에 USER_ID가 있는지 확인
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.debug("인터셉터 실행 - URI: {}, Method: {}", uri, method);

        // OPTIONS 요청은 인증 체크 스킵
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 인증 불필요한 엔드포인트
        if (isPublicEndpoint(uri, method)) {
            log.debug("인증 불필요한 엔드포인트 - URI: {}, Method: {}", uri, method);
            return true;
        }

        Long userId = sessionManager.getSession(request);

        if (userId == null) {
            log.warn("인증 실패 - 세션 없음: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"unauthorized\",\"data\":null}");
            return false;
        }

        // 인증 성공 - 컨트롤러에서 사용
        request.setAttribute("userId", userId);
        log.debug("인증 성공 - userId: {}, uri: {}", userId, uri);

        return true;
    }

    private boolean isPublicEndpoint(String uri, String method) {
        // 1. 인증 관련 (회원가입, 로그인, 중복 체크)
        if (uri.equals("/api/auth") && "POST".equals(method)) {
            return true; // 로그인
        }
        if (uri.equals("/api/auth/check-email") && "GET".equals(method)) {
            return true; // 이메일 중복 체크
        }
        if (uri.equals("/api/auth/check-nickname") && "GET".equals(method)) {
            return true; // 닉네임 중복 체크
        }

        // 2. 회원가입 (POST만 허용)
        if (uri.equals("/api/users") && "POST".equals(method)) {
            return true;
        }

        // 3. 이미지 업로드 (POST만 허용)
        if (uri.equals("/api/images") && "POST".equals(method)) {
            return true;
        }

        // 4. 게시글 목록 조회 (GET만 허용)
        if (uri.equals("/api/posts") && "GET".equals(method)) {
            return true;
        }

        // 5. 정적 리소스
        if (uri.startsWith("/temp/") || uri.startsWith("/images/") ||
                uri.startsWith("/policy/") || uri.startsWith("/css/") ||
                uri.startsWith("/js/") || uri.startsWith("/assets/")) {
            return true;
        }

        return false;
    }
}
