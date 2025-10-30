package ktb.cloud_james.community.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.global.security.JwtTokenProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * JWT 인증 필터
 * - Authorization 헤더에서 Access Token 추출 및 검증
 * - 인증 필요 없는 경우는 필터링 제외
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    // 필터링 제외 경로 목록
    private static final String[] EXCLUDED_PATHS = {
            "/api/auth",
            "/api/images",
            "/temp/",
            "/images/",
            "/policy/",
            "/css/",
            "/js/",
            "/assets/",
            "/error"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청 필터링 제외
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // POST /api/users (회원가입)만 필터 제외
        if ("/api/users".equals(path) && "POST".equalsIgnoreCase(method)) {
            return true;
        }

        // GET /api/posts (게시글 목록)만 필터 제외
        if ("/api/posts".equals(path) && "GET".equalsIgnoreCase(method)) {
            return true;
        }

        return Arrays.stream(EXCLUDED_PATHS).anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        try {
            // 1. Authorization 헤더에서 토큰 추출
            Optional<String> token = extractTokenFromHeader(request);

            if (token.isEmpty()) {
                sendUnauthorizedResponse(response, "token_missing");
                return;
            }

            // 2. 토큰 검증 및 userId 추출
            if (!validateAndSetAttributes(token.get(), request, response)) {
                return;
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT 필터 처리 중 예외 발생: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "internal_server_error");
        }
    }

    private Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }

    private boolean validateAndSetAttributes(
            String token,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                sendUnauthorizedResponse(response, "invalid_token");
                return false;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            request.setAttribute("userId", userId);

            log.debug("JWT 인증 성공 - userId: {}", userId);
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰 - URI: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "token_expired");
            return false;
        } catch (Exception e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            sendUnauthorizedResponse(response, "invalid_token");
            return false;
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException{
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
