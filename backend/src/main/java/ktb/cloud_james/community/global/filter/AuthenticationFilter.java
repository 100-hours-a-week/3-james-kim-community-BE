package ktb.cloud_james.community.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import ktb.cloud_james.community.global.util.SessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 필터
 *  - 세션 존재 여부만 확인 (로그인/비로그인 사용자 구분)
 *  - 실제 API별 접근 권한은 인터셉터가 처리
 */
@Slf4j
@Component
public class AuthenticationFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        // API 요청 아니면 통과 (정적 리소스 같은)
        if (!uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // 세션에서 userId 확인
        Long userId = SessionUtil.getUserId(httpRequest);

        if (userId != null) {
            httpRequest.setAttribute("userId", userId);
            log.debug("인증 확인 - 로그인 사용자, userId: {}", userId);
        } else {
            log.debug("인증 확인 - 비로그인 사용자");
        }

        // 필터는 세션만, 접근 제어는 인터셉터가
        chain.doFilter(request, response);
    }
}
