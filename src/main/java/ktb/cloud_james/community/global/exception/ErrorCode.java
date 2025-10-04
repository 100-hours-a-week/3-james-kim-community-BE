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

    // ========== 회원가입 관련 ==========

    // 400 Bad Request - 잘못된 요청
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "password_mismatch"),

    // 409 Conflict - 중복
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "email_already_exists"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "nickname_already_exists"),

    // 500 Internal Server Error - 서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");

    private final HttpStatus status;

    private final String message;
}
