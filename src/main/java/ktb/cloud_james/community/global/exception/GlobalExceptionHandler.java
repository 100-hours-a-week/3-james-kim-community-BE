package ktb.cloud_james.community.global.exception;

import ktb.cloud_james.community.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 * - @RestControllerAdvice: 모든 컨트롤러에서 발생하는 예외를 잡아서 처리
 * - 일관된 에러 응답 형식 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("CustomException 발생: {}", e.getMessage());

        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getMessage()));
    }

    /**
     * Validation 예외 처리
     * - @Valid에서 발생한 유효성 검증 실패
     * - DTO의 @NotBlank, @Email, @Pattern 등 검증 실패 시
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {

        // 첫 번째 필드 에러의 메시지 추출
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String errorMessage = fieldError.getDefaultMessage();

        log.error("Validation 실패: field={}, message={}", fieldError.getField(), errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * DB 제약조건 위반 예외 처리
     * - UNIQUE KEY 중복 등
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e) {

        log.error("DB 제약조건 위반", e);

        String message = e.getMessage();

        // 이메일 중복 (UNIQUE KEY: uk_users_email)
        if (message != null && message.contains("uk_users_email")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("email_already_exists"));
        }

        // 닉네임 중복 (UNIQUE KEY: uk_users_nickname)
        if (message != null && message.contains("uk_users_nickname")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("nickname_already_exists"));
        }

        // 그 외 제약조건 위반
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("invalid_request"));
    }

    /**
     * 예상하지 못한 예외 처리
     * - 위의 핸들러에서 잡히지 않은 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예상하지 못한 예외 발생", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("internal_server_error"));
    }
}
