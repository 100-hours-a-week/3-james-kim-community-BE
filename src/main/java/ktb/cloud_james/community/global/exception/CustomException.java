package ktb.cloud_james.community.global.exception;

import lombok.Getter;

/**
 * 커스텀 예외 클래스
 * - ErrorCode를 활용하여 일관된 에러 응답 제공
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
