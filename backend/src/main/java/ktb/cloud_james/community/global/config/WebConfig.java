package ktb.cloud_james.community.global.config;

import ktb.cloud_james.community.global.filter.AuthenticationFilter;
import ktb.cloud_james.community.global.interceptor.AuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * - 필터: 세션 확인 (인증 - 로그인/비로그인)
 * - 인터셉터: API 접근 권한 (인가 - URL+Method)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationFilter authenticationFilter;
    private final AuthenticationInterceptor authenticationInterceptor;

    @Value("${file.temp-dir:uploads/temp}")
    private String tempDir;

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    /**
     * 정적 리소스 핸들러 등록
     * /temp/** → uploads/temp/ (임시 이미지)
     * /images/** → uploads/images/ (정식 이미지)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 임시 이미지
        registry.addResourceHandler("/temp/**")
                .addResourceLocations("file:" + tempDir + "/");

        // 정식 이미지
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    /**
     * CORS 설정
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // /api로 시작하는 모든 경로
                .allowedOrigins("http://localhost:3000")  // 프론트엔드 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 인증 필터 등록
     * - API 요청에 대해 세션 존재 여부 확인
     * - 접근 제어 없이 userId만 request에 저장
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authenticationFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return  registration;
    }

    /**
     * 인터셉터 등록
     * - URL + Method 조합으로 실제 접근 제어
     * - Public/Private API 구분
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/**")  // 모든 API에 적용
                .excludePathPatterns(
                        // 정적 리소스 (이미지 파일)
                        "/temp/**",                         // 임시 이미지
                        "/images/**",                       // 정식 이미지

                        // 정책 페이지 등 정적 리소스
                        "/policy/**",
                        "/css/**",
                        "/js/**",
                        "/assets/**"
                );
    }
}