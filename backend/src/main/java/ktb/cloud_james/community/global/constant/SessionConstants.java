package ktb.cloud_james.community.global.constant;

/**
 * 세션 관련 상수
 * - 세션에 저장되는 키 값 정의
 */
public final class SessionConstants {

    private SessionConstants() {
    }

    // 세션에 저장되는 사용자 ID 키
    public static final String USER_ID = "USER_ID";

    // 세션 쿠키 이름
    public static final String SESSION_COOKIE_NAME = "MY_SESSION_ID";
}
