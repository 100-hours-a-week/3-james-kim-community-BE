package ktb.cloud_james.community.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 조회 응답 DTO
 * /api/users/me
 */
@Getter
@NoArgsConstructor
public class UserInfoResponseDto {

    private String email;
    private String nickname;
    private String imageUrl;

    @Builder
    public UserInfoResponseDto(String email, String nickname, String imageUrl) {
        this.email = email;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
    }
}
