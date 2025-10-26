package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.image.ImageUploadResponseDto;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이미지 업로드 관련 비즈니스 로직
 * - 임시 디렉토리에 업로드
 * - 회원가입 성공 시 정식 디렉토리로 이동
 * - 배치 작업으로 미사용 임시 파일 정리
 *
 * ========== S3 전환 계획 ==========
 * 현재: 로컬 파일 시스템 -> S3에 대한 학습이 부족해 당장 구현이 어려워 로컬로 대체해서 구현
 * 향후: AWS S3
 *
 * S3 전환 시 변경 사항:
 * - saveFile(): S3에 맞게
 * - moveToPermanent(): S3에 맞게
 * - validateFile(), createDirectory() 등 검증 로직은 동일할 것으로 예상
 * - URL 형식: /temp/abc.jpg → https://s3.amazonaws.com/bucket/temp/abc.jpg
 */
@Service
@Slf4j
public class ImageService {

    @Value("${file.temp-dir:uploads/temp}")
    private String tempDir;

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    @Value("${file.max-size:5242880}") // 5MB
    private long maxFileSize;

    public ImageUploadResponseDto uploadImageToTemp(MultipartFile file) {
        log.info("이미지 임시 업로드 시도 - filename: {}, size: {}",
                file.getOriginalFilename(), file.getSize());

        // 1. 파일 검증
        validateFile(file);

        // 2. 임시 디렉토리 생성
        createDirectory(tempDir);

        // 3. 임시 디렉토리에 파일 저장
        String savedFileName = saveFile(file, tempDir);

        // 4. 임시 URL 반환
        String tempUrl = "/temp/" + savedFileName;

        log.info("이미지 임시 업로드 성공 - url: {}", tempUrl);

        return new ImageUploadResponseDto(tempUrl);
    }

    /**
     * 파일 검증
     * - 빈 파일 체크
     * - 파일 크기 체크 (5MB 제한)
     * - 파일 형식 체크 (jpg, jpeg, png만 허용)
     *
     * 지원 형식을 3개로 제한한 이유:
     * - 웹 표준 이미지 포맷 (모든 브라우저 지원)
     * - gif는 프로필 이미지로 부적합 (용량 크고, 애니메이션 불필요)
     * - webp는 일부 구형 브라우저 미지원
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (file.getSize() > maxFileSize) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        // 대표적인 파일 타입 3개 지원
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                 !contentType.equals("image/png") &&
                 !contentType.equals("image/jpg"))) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private void createDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 파일 저장
     * - UUID로 고유한 파일명 생성
     * - 원본 확장자 유지
     *
     * UUID 사용 이유:
     * - 파일명 중복 방지 (동시에 같은 이름 업로드 시)
     * - 보안 (원본 파일명 노출 방지)
     * - URL 예측 불가 (무작위 접근 차단)
     */
    private String saveFile(MultipartFile file, String directory) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg"; // 확장자 없으면 .jpg 기본값

            // UUID로 고유한 파일명 생성
            String savedFileName = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(directory, savedFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return savedFileName;

        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 파일을 정식 디렉토리로 이동
     * - 회원가입 성공 시 호출
     * - temp/abc.jpg → images/abc.jpg
     */
    public String moveToPermanent(String tempUrl) {
        if (tempUrl == null || !tempUrl.startsWith("/temp/")) {
            return tempUrl;
        }

        try {
            // /temp/abc.jpg → abc.jpg
            String fileName = tempUrl.replace("/temp/", "");

            // 경로 설정
            Path source = Paths.get(tempDir, fileName);
            Path target = Paths.get(uploadDir, fileName);

            // 정식 디렉토리 생성
            createDirectory(uploadDir);

            // 파일 이동 (원본 삭제)
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            String permanentUrl = "/images/" + fileName;
            log.info("파일 이동 완료: {} → {}", tempUrl, permanentUrl);

            return permanentUrl;

        } catch (IOException e) {
            log.error("파일 이동 실패: {}", tempUrl, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일 삭제 (Best Effort 개념 - 예외를 던지지 않음)
     */
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/images/")) {
            return;
        }

        try {
            String fileName = imageUrl.replace("/images/", "");
            Path filePath = Paths.get(uploadDir, fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 완료 - url: {}", imageUrl);
            }
        } catch (IOException e) {
            log.error("[ORPHAN_FILE] 파일 삭제 실패 (고아 파일 발생) - url: {}", imageUrl, e);
            // 고아 파일은 별도 배치 작업이나 스케줄러로 정리
        }
    }
}
