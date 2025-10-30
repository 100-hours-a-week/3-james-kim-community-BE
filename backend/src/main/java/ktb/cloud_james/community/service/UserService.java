package ktb.cloud_james.community.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import ktb.cloud_james.community.dto.auth.NicknameCheckResponseDto;
import ktb.cloud_james.community.dto.auth.SignUpRequestDto;
import ktb.cloud_james.community.dto.auth.SignUpResponseDto;
import ktb.cloud_james.community.dto.auth.TokenDto;
import ktb.cloud_james.community.dto.user.*;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;


/**
 * 사용자(User) 관련 비즈니스 로직
 * - 회원가입, 토큰 발급, 회원정보 수정, 비밀번호 수정 등
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
    public SignUpResponseDto signUp(SignUpRequestDto request, HttpServletResponse response) {
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
            String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId());
            String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());

            // 8. Refresh Token DB 저장
            saveRefreshToken(savedUser, refreshToken);

            // 9. Refresh Token 쿠키 설정
            addRefreshTokenCookie(response, refreshToken);

            log.info("회원가입 성공 - userId: {}", savedUser.getId());

            return new SignUpResponseDto(accessToken);

        } catch (Exception e) {
            // DB 저장 실패 시 이미지 삭제 시도 (Best Effort)
            if (finalImageUrl != null) {
                imageService.deleteFile(finalImageUrl);
                log.error("회원가입 실패로 이미지 삭제 - imageUrl: {}", finalImageUrl);
            }
            throw e;
        }
    }

    /**
     * 닉네임 중복 체크 (회원정보 수정용)
     * - JWT에서 추출한 userId로 본인 제외
     * - 본인의 현재 닉네임은 중복으로 간주하지 않음
     */
    public NicknameCheckResponseDto checkNicknameAvailabilityForUpdate(
            Long userId,
            String nickname
    ) {
        log.debug("닉네임 중복 체크 (수정용) - userId: {}, nickname: {}", userId, nickname);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("닉네임 중복 체크 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 2. 본인의 현재 닉네임이면 사용 가능 (변경 안 한 것)
        if (nickname.equals(user.getNickname())) {
            log.debug("본인의 현재 닉네임 - 사용 가능: nickname={}", nickname);
            return new NicknameCheckResponseDto(true);
        }

        // 3. 다른 사람이 사용 중인지 확인 (본인 제외)
        boolean exists = userRepository.existsByNicknameAndIdNot(nickname, userId);

        log.debug("닉네임 중복 체크 (수정용) 완료 - nickname={}, available={}", nickname, !exists);

        return new NicknameCheckResponseDto(!exists);
    }

    /**
     * 사용자 정보 조회
     * - 현재 로그인한 사용자 정보 반환
     */
    public UserInfoResponseDto getUserInfo(Long userId) {
        log.info("사용자 정보 조회 - userId: {}", userId);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자 정보 조회 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        log.info("사용자 정보 조회 완료 - userId: {}, email: {}, nickname: {}",
                userId, user.getEmail(), user.getNickname());

        return UserInfoResponseDto.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .imageUrl(user.getImageUrl())
                .build();
    }

    /**
     * 회원정보 수정 처리 흐름:
     * 1. 사용자 조회 및 권한 확인
     * 2. 수정 요청 검증 (최소 하나는 수정되어야 함)
     * 3. 닉네임 중복 체크 (본인 제외)
     * 4. 이미지 처리 (수정/삭제/유지)
     * 5. 사용자 정보 업데이트
     * 6. 실패 시 이미지 롤백
     */
    @Transactional
    public UserUpdateResponseDto updateUser(Long userId, UserUpdateRequestDto request) {
        log.info("회원정보 수정 시도 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원정보 수정 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 2. 수정 요청 검증 (최소 하나는 수정되어야 함)
        if (!request.hasAnyUpdate()) {
            log.warn("회원정보 수정 실패 - 수정할 내용 없음: userId={}", userId);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 3. 닉네임 중복 체크 (본인 제외)
        if (request.getNickname() != null) {
            // 기존 닉네임과 같으면 중복 체크 안 함
            if (!request.getNickname().equals(user.getNickname())) {
                if (userRepository.existsByNicknameAndIdNot(request.getNickname(), userId)) {
                    log.warn("회원정보 수정 실패 - 닉네임 중복: nickname={}", request.getNickname());
                    throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
                }
            }
        }

        // 4. 이미지 처리
        String finalImageUrl = handleImageUpdate(user, request.getImageUrl());

        try {
            // 5. 사용자 정보 업데이트 (JPA Dirty Checking)
            updateUserFields(user, request, finalImageUrl);

            log.info("회원정보 수정 완료 - userId: {}", userId);

            return new UserUpdateResponseDto(userId);
        } catch (Exception e) {
            // 실패 시 새로 이동한 이미지 삭제
            if (finalImageUrl != null && finalImageUrl.startsWith("/images/")) {
                imageService.deleteFile(finalImageUrl);
                log.error("회원정보 수정 실패로 이미지 삭제 - imageUrl: {}", finalImageUrl);
            }
            throw e;
        }
    }

    /**
     * 비밀번호 수정 처리 흐름:
     * 1. 사용자 조회 및 권한 확인
     * 2. 새 비밀번호 일치 여부 검증
     * 3. 비밀번호 암호화 및 업데이트
     * 4. password_changed_at 갱신
     */
    @Transactional
    public PasswordUpdateResponseDto updatePassword(
            Long userId,
            PasswordUpdateRequestDto request
    ) {
        log.info("비밀번호 수정 시도 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("비밀번호 수정 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 2. 새 비밀번호 일치 여부 검증 (이중 검증)
        // 프론트에서 1차 실시간 검증, 서버에서 2차 검증
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            log.warn("비밀번호 수정 실패 - 비밀번호 불일치: userId={}", userId);
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // 4. 비밀번호 업데이트 (JPA Dirty Checking)
        user.updatePassword(encodedPassword);

        log.info("비밀번호 수정 완료 - userId: {}", userId);

        return new PasswordUpdateResponseDto(userId);
    }

    /**
     * 회원탈퇴 처리 흐름:
     * 1. 사용자 조회
     * 2. 이미 탈퇴한 회원인지 확인
     * 3. User Soft Delete (deleted_at 기록, is_active = false)
     * 4. Refresh Token 삭제
     * 5. 프로필 이미지 삭제
     *
     * 참고:
     * - User의 게시글/댓글은 유지 (작성자 표시: "탈퇴한 회원")
     */
    @Transactional
    public void withdrawUser(Long userId, HttpServletResponse response) {
        log.info("회원탈퇴 시도 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("회원탈퇴 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 2. 이미 탈퇴한 회원인지 확인
        if (user.isDeleted()) {
            log.warn("회원탈퇴 실패 - 이미 탈퇴한 회원: userId={}", userId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. User Soft Delete
        user.withdraw(); // deleted_at 기록, is_active = false
        log.info("User Soft Delete 완료 - userId: {}", userId);

        // 4. Refresh Token 삭제 (로그아웃 처리)
        userTokenRepository.findByUser(user)
                .ifPresent(userToken -> {
                    userTokenRepository.delete(userToken);
                    log.info("Refresh Token 삭제 완료 - userId: {}", userId);
                });

        deleteRefreshTokenCookie(response);

        // 5. 프로필 이미지 삭제
        if (user.getImageUrl() != null) {
            try {
                imageService.deleteFile(user.getImageUrl());
                log.info("프로필 이미지 삭제 완료 - userId: {}, imageUrl: {}", userId, user.getImageUrl());
            } catch (Exception e) {
                // 이미지 삭제 실패는 치명적이지 않으므로 경고만 로그
                log.warn("프로필 이미지 삭제 실패 (고아 파일 발생) - userId: {}, imageUrl: {}", userId, user.getImageUrl(), e);
            }
        }

        log.info("회원탈퇴 완료 - userId: {}", userId);
    }



    /**
     * Refresh Token DB 저장
     */
    private void saveRefreshToken(User user, String refreshToken) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenValidity() / 1000);

        // 기존 토큰 있으면 삭제
        userTokenRepository.findByUser(user).ifPresent(userTokenRepository::delete);

        // Refresh Token 암호화 (SHA-256 해시)
        String hashedRefreshToken = hashRefreshToken(refreshToken);

        // 새 Refresh Token 저장
        UserToken userToken = UserToken.builder()
                .user(user)
                .refreshToken(hashedRefreshToken)
                .expiresAt(expiresAt)
                .build();

        userTokenRepository.save(userToken);
        log.debug("Refresh Token 저장 완료 - userId: {}", user.getId());
    }

    /**
     * RefreshToken SHA-256 해싱
     */
    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 알고리즘을 찾을 수 없음", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * RefreshToken 쿠키 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTPS 환경에서는 true로 설정
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(cookie);
    }

    /**
     * RefreshToken 쿠키 삭제
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
    }

    /**
     * 이미지 처리 로직
     * - null: 수정 안 함 (기존 이미지 유지) → null 반환
     * - "": 기존 이미지 삭제 → "" 반환 (빈 문자열)
     * - "/temp/...": 새 이미지로 교체 → 이동된 이미지 URL 반환
     */
    private String handleImageUpdate(User user, String requestImageUrl) {
        // 이미지 수정 요청이 없으면 null 반환 (변경 없음)
        if (requestImageUrl == null) {
            return null;
        }

        // 빈 문자열이면 기존 이미지 삭제
        if (requestImageUrl.isEmpty()) {
            // 기존 이미지가 있으면 삭제
            if (user.getImageUrl() != null) {
                imageService.deleteFile(user.getImageUrl());
                log.info("기존 프로필 이미지 삭제 - userId: {}, imageUrl: {}",
                        user.getId(), user.getImageUrl());
            }
            return ""; // 빈 문자열 반환 (이미지 삭제됨)
        }

        // 임시 이미지면 정식 디렉토리로 이동
        if (requestImageUrl.startsWith("/temp/")) {
            try {
                String finalImageUrl = imageService.moveToPermanent(requestImageUrl);
                log.info("프로필 이미지 이동 완료 - {} → {}", requestImageUrl, finalImageUrl);

                // 기존 이미지가 있으면 삭제
                if (user.getImageUrl() != null) {
                    imageService.deleteFile(user.getImageUrl());
                    log.info("기존 프로필 이미지 삭제 - imageUrl: {}", user.getImageUrl());
                }

                return finalImageUrl;
            } catch (Exception e) {
                log.error("프로필 이미지 이동 실패 - tempUrl: {}", requestImageUrl, e);
                throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }

        // /temp/로 시작하지 않으면 잘못된 요청
        log.warn("잘못된 이미지 URL - userId: {}, imageUrl: {}", user.getId(), requestImageUrl);
        throw new CustomException(ErrorCode.INVALID_REQUEST);
    }

    /**
     * 사용자 정보 업데이트
     */
    private void updateUserFields(User user, UserUpdateRequestDto request, String finalImageUrl) {
        // 닉네임 수정
        if (request.getNickname() != null) {
            user.updateNickname(request.getNickname());
            log.debug("닉네임 수정 - userId: {}, nickname: {}", user.getId(), request.getNickname());
        }

        // 이미지 처리
        if (finalImageUrl != null) {
            if (finalImageUrl.isEmpty()) {
                // 이미지 삭제
                user.updateImageUrl(null);
                log.debug("프로필 이미지 삭제 완료 - userId: {}", user.getId());
            } else {
                // 새 이미지 저장
                user.updateImageUrl(finalImageUrl);
                log.debug("새 프로필 이미지 저장 완료 - userId: {}, imageUrl: {}", user.getId(), finalImageUrl);
            }
        }
    }
}
