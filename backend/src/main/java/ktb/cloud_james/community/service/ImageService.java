package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.image.ImageUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 서비스 인터페이스
 * - dev: LocalImageService
 * - prod: S3ImageService
 * - 구현체는 @Profile로 주입
 */
public interface ImageService {

    // 임시 이미지 업로드
    ImageUploadResponseDto uploadImageToTemp(MultipartFile file);

    // 정식 이미지로 이동
    String moveToPermanent(String tempImageUrl);

    // 이미지 삭제
    void deleteFile(String imageUrl);
}
