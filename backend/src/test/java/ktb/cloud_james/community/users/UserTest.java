package ktb.cloud_james.community.users;

import ktb.cloud_james.community.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

    @Test
    @DisplayName("User 객체 생성")
    void createUser() {
        // given
        String email = "test@example.com";
        String password = "testPassword123!";
        String nickname = "Tester";

        // when
        User user = User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .imageUrl(null)
                .build();

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getImageUrl()).isEqualTo(null);
        assertThat(user.getIsActive()).isEqualTo(true);
    }
}

