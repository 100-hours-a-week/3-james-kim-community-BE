package ktb.cloud_james.community.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 임시 파일 정리 스케줄러 (회원가입 중)
 * - 1시간 지난 임시 파일 자동 삭제
 */
@Component
@Slf4j
public class FileCleanupScheduler {

    @Value("${file.temp-dir:uploads/temp}")
    private String tempDir;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldTempFiles() {
        log.info("========== 임시 파일 정리 시작 ==========");

        File tempDirectory = new File(tempDir);

        // 디렉토리 없으면 종료
        if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
            log.warn("임시 디렉토리가 존재하지 않음: {}", tempDir);
            return;
        }

        // 1시간 전 시간 계산
        long cutoffTime = System.currentTimeMillis() - (60 * 60 * 1000);

        int deletedCount = 0;
        long deletedSize = 0;

        File[] files = tempDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    long fileSize = file.length();

                    if (file.delete()) {
                        deletedCount++;
                        deletedSize += fileSize;
                        log.info("삭제: {} ({}KB)", file.getName(), fileSize / 1024);
                    } else {
                        log.warn("삭제 실패: {}", file.getName());
                    }
                }
            }
        }

        log.info("========== 임시 파일 정리 완료 ==========");
        log.info("삭제된 파일: {}개, 확보된 용량: {}MB",
                deletedCount, deletedSize / (1024 * 1024));
    }
}
