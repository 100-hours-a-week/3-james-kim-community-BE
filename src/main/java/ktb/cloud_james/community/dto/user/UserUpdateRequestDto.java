package ktb.cloud_james.community.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원정보 수정 요청 DTO
 * - 닉네임, 프로필 이미지 수정
 * - 둘 다 선택사항이지만, 최소 하나는 수정되어야 함 (Service에서 검증)
 */
@Getter
@NoArgsConstructor
public class UserUpdateRequestDto {

    /**
     * 닉네임 (선택)
     * - null: 수정 안 함
     * - 값 있음: 닉네임 수정
     */
    @Size(max = 10, message = "닉네임은 최대 10자까지 작성 가능합니다.")
    @Pattern(regexp = "^\\S+$", message = "띄어쓰기는 불가능합니다.")
    private String nickname;

    /**
     * 프로필 이미지 URL (선택)
     * - null: 수정 안 함 (기존 이미지 유지)
     * - "": 기존 이미지 삭제
     * - "/temp/...": 새 이미지로 교체
     */
    private String imageUrl;

    /**
     * 수정할 내용이 있는지 확인
     * - 닉네임이나 이미지 중 최소 하나는 있어야 함
     */
    public boolean hasAnyUpdate() {
        return nickname != null || imageUrl != null;
    }
}
