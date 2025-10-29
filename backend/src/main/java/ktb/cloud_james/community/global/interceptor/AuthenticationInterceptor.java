package ktb.cloud_james.community.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인가 인터셉터
 * - URL + HTTP Method 조합으로 접근 권한 판단
 * - Public API: 비로그인도 접근 가능
 * - Private API: 로그인 필요
 * - (향후 확장) ADMIN 전용 API: 관리자 권한 필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        Long userId = (Long) request.getAttribute("userId");

        log.debug("인가 확인 - URI: {}, Method: {}, userId: {}", uri, method, userId);

        // OPTIONS 요청은 인증 체크 스킵
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // public API (비로그인 허용)
        if (isPublicEndpoint(uri, method)) {
            log.debug("인증 불필요한 엔드포인트 - URI: {}, Method: {}", uri, method);
            return true;
        }

        // private API인데 비로그인 상태
        if (userId == null) {
            log.warn("인가 실패 - 세션 없음, 로그인 필요: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"unauthorized\",\"data\":null}");
            return false;
        }

        // 인가 성공 - 컨트롤러에서 사용
        log.debug("private API 접근 허용 - userId: {}, uri: {}", userId, uri);

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

        return false;
    }
}
