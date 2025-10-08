package ktb.cloud_james.community.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 26, message = "제목은 최대 26자까지 작성 가능합니다.")
    private String title;

    // LONGTEXT 타입이므로 길이 제한 없음
    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // null 가능
    private String imageUrl;
}
