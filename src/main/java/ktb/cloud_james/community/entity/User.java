package ktb.cloud_james.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 나중에 체크해야 할 사항
 * UserRole (ADMIN, USER) 꼭 필요한가?
 * 우선 이 프로젝트에서는 포인트가 아니므로 기능 1차 완성 후 추가 고민.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용 기본 생성자 자동 생성
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL DB의 AUTO_INCREMENT 사용
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    // 닉네임 최대 10자 제한
    @Column(name = "nickname", nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // 기본값 = true: 객체 생성 시 활성 상태 유저로 시작
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // @CreationTimestamp: INSERT 시 현재 시간 자동 입력
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // @UpdateTimestamp: UPDATE 시마다 현재 시간으로 자동 갱신
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String nickname, String imageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
    }

    // ========== 비즈니스 메서드 ==========

    // 닉네임 수정
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 프로필 이미지 수정
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // 비밀번호 수정
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
    }
}
