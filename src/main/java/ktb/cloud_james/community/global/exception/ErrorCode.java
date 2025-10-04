package ktb.cloud_james.community.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의 (Enum)
 * - 각 예외 상황에 대한 HTTP 상태 코드와 메시지 관리
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== 공통 ==========
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error"),

    // ========== 회원가입 관련 ==========
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "password_mismatch"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "email_already_exists"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "nickname_already_exists"),

    // ========== 이미지 업로드 관련 ==========
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "file_too_large"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_file_type"),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "image_upload_failed"),

    // ========== 토큰 관련 ==========
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "invalid_token"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token_expired");

    private final HttpStatus status;

    private final String message;
}
