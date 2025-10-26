package ktb.cloud_james.community.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 * - JWT 기반 인증 (Stateless)
 * - CSRF 비활성화 (REST API용)
 * - 세션 사용 안 함
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Security 필터 체인 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용 시 불필요) -> REST API는 stateless하므로 CSRF 공격 대상이 아님
                .csrf(AbstractHttpConfigurer::disable)

                // form 로그인 방식 비활성화 -> HTML Form이 아닌 JSON 기반 API 사용
                .formLogin(AbstractHttpConfigurer::disable)

                // http basic 인증 방식 비활성화 -> Authorization: Basic 방식 대신 Bearer 토큰 사용
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy((SessionCreationPolicy.STATELESS)))

                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 회원가입, 로그인은 누구나 접근 가능
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll() // 회원가입만
                        .requestMatchers(HttpMethod.POST, "/api/images").permitAll() // 이미지 업로드만
                        .requestMatchers(HttpMethod.GET, "/api/posts").permitAll() // 게시글 홈만
                        .requestMatchers("/temp/**", "/images/**").permitAll() // 정적 리소스
                        .requestMatchers("/policy/**").permitAll() // 정적 리소스
                        .requestMatchers("/css/**", "/js/**", "/assets/**").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터 추가 -> 모든 요청에서 JWT 검증 후 인증 처리
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    /**
     * CORS 설정
     * - 프론트엔드(localhost:3000)에서의 요청 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 노출할 헤더
        configuration.setExposedHeaders(List.of("*"));

        // 자격 증명 허용 (쿠키 등)
        configuration.setAllowCredentials(true);

        // Preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }


    /**
     * 비밀번호 암호화를 위한 PasswordEncoder
     * BCrypt: 단방향 해시 함수 (salt 자동 생성)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
