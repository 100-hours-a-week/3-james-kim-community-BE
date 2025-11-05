package ktb.cloud_james.community.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import ktb.cloud_james.community.dto.image.ImageUploadResponseDto;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * S3 기반 이미지 업로드 서비스 구현체
 * - 운영 환경(prod)에서 사용
 * - AWS S3에 이미지 저장
 * - S3 URL 반환
 */
@Service
@Profile("prod")
@Slf4j
@RequiredArgsConstructor
public class S3ImageService implements ImageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    // S3에 저장할 디렉토리 경로
    @Value("${cloud.aws.s3.temp-dir:temp}")
    private String tempDir;  // 임시 이미지 디렉토리 (기본값: temp)

    @Value("${cloud.aws.s3.upload-dir:images}")
    private String uploadDir;  // 정식 이미지 디렉토리 (기본값: images)

    @Value("${file.max-size:5242880}")
    private long maxFileSize;

    /**
     * 임시 이미지 업로드 (회원가입 중)
     * - S3의 temp/ 디렉토리에 저장
     * - UUID를 사용한 고유 파일명 생성
     * - 업로드 후 S3 URL 반환
     *
     * 동작 방식:
     * 1. 파일 검증 (크기, 형식)
     * 2. UUID 기반 파일명 생성
     * 3. S3 키 생성 (temp/uuid.jpg)
     * 4. S3에 업로드 (공개 읽기 권한)
     * 5. S3 URL 반환
     */
    @Override
    public ImageUploadResponseDto uploadImageToTemp(MultipartFile file) {
        log.info("이미지 임시 업로드 시도 - filename: {}, size: {}",
                file.getOriginalFilename(), file.getSize());

        // 1. 파일 검증
        validateFile(file);

        // 2. 고유한 파일명 생성 (UUID + 원본 확장자)
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + "." + extension;

        // 3. S3 저장 경로 생성 (temp/UUID.jpg 형태)
        String s3Key = tempDir + "/" + uniqueFilename;

        // 4. S3에 업로드 후 URL 반환
        String imageUrl = uploadToS3(file, s3Key);

        log.info("S3 임시 이미지 업로드 완료: {}", imageUrl);
        return new ImageUploadResponseDto(imageUrl);
    }

    /**
     * 정식 이미지로 이동
     * - S3의 temp/에서 images/로 복사
     * - 원본 temp/ 파일은 삭제
     *
     * 동작 방식:
     * 1. null/빈 값 체크
     * 2. S3 URL에서 키 추출
     * 3. 새로운 키 생성 (images/uuid.jpg)
     * 4. S3 내부 복사
     * 5. 원본 파일 삭제
     * 6. 새로운 URL 반환
     */
    @Override
    public String moveToPermanent(String tempImageUrl) {
        if (tempImageUrl == null || tempImageUrl.isEmpty()) {
            return null;
        }

        log.info("임시 URL: {}", tempImageUrl);

        try {
            // 1. URL에서 S3 키 추출
            String tempKey = extractS3KeyFromUrl(tempImageUrl);

            // 2. 파일명 추출 (temp/uuid.jpg → uuid.jpg)
            String filename = tempKey.substring(tempKey.lastIndexOf("/") + 1);

            // 3. 새로운 S3 키 생성 (images/uuid.jpg)
            String newKey = uploadDir + "/" + filename;

            // 4. S3 내부에서 복사 (Copy) - 다운로드/업로드 없이 S3 서버 내부에서 처리
            amazonS3.copyObject(bucketName, tempKey, bucketName, newKey);

            // 5. 원본 임시 파일 삭제
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, tempKey));

            // 6. 새로운 URL 생성 및 반환
            String newImageUrl = amazonS3.getUrl(bucketName, newKey).toString();
            log.info("이미지 이동 완료: {}", newImageUrl);

            return newImageUrl;

        } catch (Exception e) {
            log.error("S3 이미지 이동 실패: {}", tempImageUrl, e);
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 파일 삭제 (Best Effort)
     * - 게시글 이미지 수정 시 기존 이미지 삭제
     * - 고아 파일 발생 시 S3 Lifecycle Policy로 정리
     */
    @Override
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // URL에서 S3 키 추출
            String s3Key = extractS3KeyFromUrl(imageUrl);

            // S3에서 파일 삭제
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, s3Key));
            log.info("S3 파일 삭제 완료: {}", s3Key);

        } catch (Exception e) {
            // 고아 파일(orphan file)이 발생하더라도 전체 작업은 성공으로 처리
            // S3 Lifecycle Policy로 오래된 미사용 파일 정리 가능
            log.error("[ORPHAN_FILE] S3 파일 삭제 실패 (고아 파일 발생): {}", imageUrl, e);
        }
    }

    /**
     * S3에 파일 업로드 핵심 로직
     * - MultipartFile을 S3에 업로드
     * - 메타데이터 설정 (Content-Type, Content-Length)
     */
    private String uploadToS3(MultipartFile file, String s3Key) {
        try {
            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());  // MIME 타입 설정 (image/jpeg 등)
            metadata.setContentLength(file.getSize());       // 파일 크기 설정 (필수)

            // S3에 파일 업로드 요청 생성
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,                // 버킷 이름
                    s3Key,                     // S3 저장 경로 (키)
                    file.getInputStream(),     // 파일 데이터 스트림
                    metadata                   // 메타데이터
            );

            // 실제 업로드 실행
            amazonS3.putObject(putObjectRequest);

            return amazonS3.getUrl(bucketName, s3Key).toString();

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", s3Key, e);
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 파일 검증
     * - null 체크
     * - 파일 크기 검증 (5MB 제한)
     * - 파일 확장자 검증 (jpg, jpeg, png만 허용)
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 파일 크기 검증 (5MB 초과 시 에러)
        if (file.getSize() > maxFileSize) {
            log.warn("파일 크기 초과: {}bytes (제한: {}bytes)", file.getSize(), maxFileSize);
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        // Content-Type 검증 (jpg, jpeg, png만 허용)
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                        !contentType.equals("image/png") &&
                        !contentType.equals("image/jpg"))) {
            log.warn("지원하지 않는 파일 형식: {}", contentType);
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    /**
     * 파일 확장자 추출
     * - "profile.jpg" → "jpg"
     * - 확장자 없으면 에러
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * S3 URL에서 키(경로) 추출
     */
    private String extractS3KeyFromUrl(String url) {
        int bucketIndex = url.indexOf(bucketName);
        if (bucketIndex != -1) {
            return url.substring(url.indexOf("/", bucketIndex + bucketName.length()) + 1);
        } else {
            return url.substring(url.indexOf(".com/") + 5);
        }
    }
}