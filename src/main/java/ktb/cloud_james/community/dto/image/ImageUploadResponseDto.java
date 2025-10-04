package ktb.cloud_james.community.dto.image;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 이미지 업로드 응답 DTO
 * - 임시 저장된 이미지 URL 반환
 */
@Getter
@AllArgsConstructor
public class ImageUploadResponseDto {

    /**
     * 업로드된 이미지 URL (임시)
     * - /temp/abc-123.jpg 형식
     * - 회원가입 성공 시 /images/로 이동됨
     */
    private String imageUrl;
}
