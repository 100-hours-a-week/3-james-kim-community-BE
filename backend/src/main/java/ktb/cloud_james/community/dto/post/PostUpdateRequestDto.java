package ktb.cloud_james.community.dto.post;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 수정 요청 DTO
 * - 제목/내용 빈 문자열 불가
 * - 이미지 null이면 변경 없음, 빈 문자열("")이면 삭제
 */
@Getter
@NoArgsConstructor
public class PostUpdateRequestDto {

    /**
     * 제목 (선택)
     * - null: 수정 안 함
     * - 빈 문자열/공백: 불가 (Validation)
     * - 1~26자: 수정
     */
    @Size(min = 1, max = 26, message = "제목은 최소 1자, 최대 26자까지 작성 가능합니다.")
    private String title;

    /**
     * 내용 (선택)
     * - null: 수정 안 함
     * - 빈 문자열/공백: 불가 (Validation)
     * - 값 있음: 수정
     */
    @Size(min = 1, message = "내용을 입력해주세요.")
    private String content;

    /**
     * 이미지 수정/삭제 (선택)
     * - null: 수정 안 함 (기존 이미지 유지)
     * - "": 기존 이미지 삭제
     * - newUrl("/temp/..."): 새 이미지로 교체
     */
    private String imageUrl;

    /**
     * 수정 요청 검증
     * - 최소 1개 필드는 변해야 수정됨
     */
    public boolean hasAnyUpdate() {
        return title != null || content != null || imageUrl != null;
    }
}
