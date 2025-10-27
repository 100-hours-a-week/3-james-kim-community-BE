package ktb.cloud_james.community.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ktb.cloud_james.community.global.util.SessionUtil;
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
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        // OPTIONS 요청은 인증 체크 스킵
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = SessionUtil.getUserId(request);

        if (userId == null) {
            log.warn("인증 실패 - 세션 없음: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"unauthorized\",\"data\":null}");
            return false;
        }

        // 인증 성공 - 컨트롤러에서 사용
        request.setAttribute("userId", userId);
        log.debug("인증 성공 - userId: {}, uri: {}", userId, request.getRequestURI());

        return true;
    }
}
