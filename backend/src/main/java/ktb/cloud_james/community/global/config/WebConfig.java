package ktb.cloud_james.community.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 -> S3 쓰면 완전히 제거 하면 된다. 임시로 필요
 * - 정적 리소스 경로 설정
 * - 업로드된 이미지 파일을 HTTP로 제공
 */
@Configuration
@Profile("dev")
public class WebConfig implements WebMvcConfigurer {

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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // /api로 시작하는 모든 경로
                .allowedOrigins("http://localhost:3000")  // 프론트엔드 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}