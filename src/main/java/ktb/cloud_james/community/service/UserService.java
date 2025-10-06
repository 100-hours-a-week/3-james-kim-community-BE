package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.dto.auth.TokenDto;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.entity.UserToken;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.global.security.JwtTokenProvider;
import ktb.cloud_james.community.repository.UserRepository;
import ktb.cloud_james.community.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사용자(User) 관련 비즈니스 로직
 * - 회원가입, 토큰 발급 등
 *
 * @Transactional(readOnly = true):
 * - 클래스 레벨: 모든 메서드에 읽기 전용 트랜잭션 적용
 * - 조회 성능 최적화 (Dirty Checking 비활성화)
 * - 쓰기 작업이 필요한 메서드는 @Transactional로
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ImageService imageService;
    private final AuthService authService;

    /**
     * 회원가입 처리 흐름:
     * 1. 비밀번호 일치 검증
     * 2. 이메일/닉네임 중복 체크
     * 3. 비밀번호 암호화
     * 4. 임시 이미지 → 정식 디렉토리 이동
     * 5. User 저장
     * 6. Access Token, Refresh Token 발급
     * 7. Refresh Token DB 저장
     * 8. 실패 시 이동한 이미지 삭제
     */
    @Transactional
    public SignUpResponseDto signUp(SignUpRequestDto request) {
        log.info("회원가입 시도 - email: {}, nickname: {}", request.getEmail(), request.getNickname());

        // 1. 비밀번호 일치 검증
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. 임시 이미지 -> 정식 디렉토리 이동
        String finalImageUrl = null;
        String tempImageUrl = request.getProfileImage();

        if (tempImageUrl != null && tempImageUrl.startsWith("/temp/")) {
            try {
                finalImageUrl = imageService.moveToPermanent(tempImageUrl);
            } catch (Exception e) {
                log.error("이미지 이동 실패 - tempUrl: {}", tempImageUrl, e);
                throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }

        try {
            // 6. User 엔티티 생성 및 저장
            User user = User.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .nickname(request.getNickname())
                    .imageUrl(finalImageUrl)
                    .build();

            User savedUser = userRepository.save(user);

            // 7. 토큰 발급
            TokenDto tokens = generateTokens(savedUser.getId());

            // 8. Refresh Token DB 저장
            authService.saveRefreshToken(savedUser, tokens.getRefreshToken());

            log.info("회원가입 성공 - userId: {}", savedUser.getId());

            return new SignUpResponseDto(
                    savedUser.getId(),
                    tokens.getAccessToken(),
                    tokens.getRefreshToken());

        } catch (Exception e) {
            // DB 저장 실패 시 이미지 삭제 시도 (Best Effort)
            imageService.deleteFile(finalImageUrl);
            throw e;
        }
    }

    private TokenDto generateTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        log.debug("토큰 생성 완료 - userId: {}", userId);

        return new TokenDto(accessToken, refreshToken);
    }
}
