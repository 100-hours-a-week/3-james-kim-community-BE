package ktb.cloud_james.community.users;

import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.global.security.JwtTokenProvider;
import ktb.cloud_james.community.repository.UserRepository;
import ktb.cloud_james.community.service.AuthService;
import ktb.cloud_james.community.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 - 토큰 검증")
    void signup_success() {
        // given
        SignUpRequestDto request = SignUpRequestDto.builder()
                .email("test@example.com")
                .password("testPassword123!")
                .passwordConfirm("testPassword123!")
                .nickname("Tester")
                .profileImage(null)
                .build();

        String encodedPassword = "encodedPassword123!";
        String accessToken = "mockAccessToken";
        String refreshToken = "mockRefreshToken";

        User savedUser = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .imageUrl(null)
                .build();

        ReflectionTestUtils.setField(savedUser, "id", 1L);

        // 이메일/닉네임 중복 없음 시나리오 테스트
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.createAccessToken(any())).thenReturn(accessToken);
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn(refreshToken);

        // when: 회원가입 동작 실행
        SignUpResponseDto response = userService.signUp(request);

        // then: 토큰 반환 검증
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);

        // 호출 검증
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, times(1)).existsByNickname(request.getNickname());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 시 예외 발생")
    void signup_emailDuplicate_error() {
        // given
        SignUpRequestDto request = SignUpRequestDto.builder()
                .email("test@example.com")
                .password("testPassword123!")
                .passwordConfirm("testPassword123!")
                .nickname("Tester")
                .profileImage(null)
                .build();

        // 이메일 중복 예외 발생
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // when
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        // then
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, never()).existsByNickname(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복 시 예외 발생")
    void signup_nicknameDuplicate_error() {
        // given
        SignUpRequestDto request = SignUpRequestDto.builder()
                .email("test@example.com")
                .password("testPassword123!")
                .passwordConfirm("testPassword123!")
                .nickname("Tester")
                .profileImage(null)
                .build();

        // 닉네임 중복 예외 발생
        when(userRepository.existsByNickname(request.getNickname())).thenReturn(true);

        // when
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXISTS);

        // then
        verify(userRepository, times(1)).existsByNickname(request.getNickname());
        verify(userRepository, never()).save(any(User.class));
    }

}
