package ktb.cloud_james.community.global.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 * - 모든 HTTP 요청에서 JWT 토큰 검증
 * - 유효한 토큰이면 SecurityContext에 인증 정보 저장
 * - Controller에서 @AuthenticationPrincipal로 userId 접근 가능하게 함
 *
 * OncePerRequestFilter:
 * - 요청당 한 번만 실행 보장
 * - Spring Security 표준 필터 패턴
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;


    /**
     * 필터 핵심 로직
     *
     * 실행 흐름:
     * 1. Authorization 헤더에서 JWT 추출
     * 2. 토큰 유효성 검증
     * 3. 유효하면 SecurityContext에 인증 정보 저장
     * 4. 다음 필터로 요청 전달
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 1. 헤더에서 JWT 토큰 추출
            String token = resolveToken(request);

            // 2. 토큰 검증 및 인증
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                /**
                 * 인증 객체 생성
                 * - principal: 사용자 ID
                 * - credentials: 비밀번호 (JWT는 불필요하므로 null)
                 * - authorities: 권한 목록 (현재는 역할 구분 없으므로 빈 리스트)
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 3. SecurityContext에 인증 정보 저장
                // 이후 Controller에서 @AuthenticationPrincipal로 접근 가능
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("사용자 인증 성공 - userId: {}", userId);
            }
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰 - URI: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"token_expired\",\"data\":null}");

            return;
        } catch (Exception e) {
            log.error("JWT 인증 실패: {}", e.getMessage());
        }

        // 4. 다음 필터로 요청 전달 (필수!)
        filterChain.doFilter(request, response);
    }

    // HTTP 헤더에서 JWT 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
