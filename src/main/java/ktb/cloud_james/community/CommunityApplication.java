package ktb.cloud_james.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling: 스케줄링 기능 활성화
 * - @Scheduled 어노테이션 사용 가능
 * - FileCleanupScheduler의 배치 작업 실행
 */
@SpringBootApplication
@EnableScheduling
public class CommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
