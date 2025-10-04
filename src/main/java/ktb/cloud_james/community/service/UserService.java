package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자(User) 관련 비즈니스 로직
 * - 회원가입 등
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
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     * - 비밀번호 일치 검증
     * - 이메일/닉네임 중복 체크
     * - 비밀번호 암호화
     * - 사용자 저장
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

        // 5. User 엔티티 생성 및 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .imageUrl(request.getProfileImage())
                .build();

        User savedUser = userRepository.save(user);

        log.info("회원가입 성공 - userId: {}", savedUser.getId());

        return new SignUpResponseDto(savedUser.getId());
    }
}
