package ktb.cloud_james.community.controller;

import ktb.cloud_james.community.dto.common.ApiResponse;
import ktb.cloud_james.community.dto.image.ImageUploadResponseDto;
import ktb.cloud_james.community.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 API 컨트롤러
 * - 회원가입 시 프로필 이미지 업로드
 */
@RestController
@RequestMapping("/api/images")
@Slf4j
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImageUploadResponseDto>> uploadImage(
            @RequestParam("file")MultipartFile file) {

        ImageUploadResponseDto response = imageService.uploadImageToTemp(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("upload_success", response));
    }
}
