package ktb.cloud_james.community.global.config;

import ktb.cloud_james.community.global.interceptor.AuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 -> S3 쓰면 완전히 제거 하면 된다. 임시로 필요
 * - 정적 리소스 경로 설정
 * - 업로드된 이미지 파일을 HTTP로 제공
 * - 인증 인터셉터 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

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
     * 인터셉터 등록
     * - 기존 Spring Security 설정과 동일하게 URL별 접근 권한 설정
     * - 인증이 필요한 API에만 인터셉터 적용
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/**")  // 모든 API에 적용
                .excludePathPatterns(
                        //인증 불필요한 경로
                        // 인증 관련 (회원가입, 로그인, 중복 체크)
                        "/api/auth",                        // POST: 로그인
                        "/api/auth/check-email",            // GET: 이메일 중복 체크
                        "/api/auth/check-nickname",         // GET: 닉네임 중복 체크 (회원가입용)

                        "/api/users",                       // POST: 회원가입만 허용 (GET, PATCH, DELETE는 인증 필요)
                        "/api/images",                      // POST: 이미지 업로드만 허용
                        "/api/posts",                       // GET: 게시글 목록만 허용 (POST는 인증 필요)

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