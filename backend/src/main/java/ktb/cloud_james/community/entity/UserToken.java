package ktb.cloud_james.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용 기본 생성자 자동 생성
public class UserToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL DB의 AUTO_INCREMENT 사용
    @Column(name = "token_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * @ManyToOne: N:1 관계 (UserToken 여러 개 -> User 한 명)
     * fetch = FetchType.LAZY: 지연 로딩
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // @CreationTimestamp: INSERT 시 현재 시간 자동 입력
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserToken(User user, String refreshToken, LocalDateTime expiresAt) {
        this.user = user;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
