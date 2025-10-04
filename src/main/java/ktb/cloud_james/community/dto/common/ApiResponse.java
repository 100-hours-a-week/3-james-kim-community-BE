package ktb.cloud_james.community.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 공통 API 응답 형식
 * - 모든 API 응답을 통일된 형태로 반환
 *
 * 응답 예시:
 * {
 *   "message": "signup_success",
 *   "data": {
 *     "user_id": 1
 *   }
 * }
 */
@Getter
public class ApiResponse<T> {

    private final String message;

    /**
     * 응답 데이터
     * - 실제 반환할 데이터
     * - 제네릭 타입으로 처리
     * - null이면 JSON에서 제외 (@JsonInclude)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    private ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null);
    }
}