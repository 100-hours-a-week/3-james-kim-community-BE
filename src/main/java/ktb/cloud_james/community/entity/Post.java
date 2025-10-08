package ktb.cloud_james.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용 기본 생성자 자동 생성
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL DB의 AUTO_INCREMENT 사용
    @Column(name = "post_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 26)
    private String title;

    /**
     * @Lob + columnDefinition
     * JPA에게 이건 대용량 데이터야라고 알림 + 정확한 DB 타입 지정
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /**
     * @ManyToOne: N:1 관계 (Post 여러 개 -> User 한 명)
     * fetch = FetchType.LAZY: 지연 로딩
     *   - Post 조회 시 User는 즉시 가져오지 않음
     *   - post.getUser()를 실제로 호출할 때 쿼리 실행
     *   - EAGER(즉시 로딩)는 N+1 문제 발생 가능성 높음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    public Post(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
    }

    // ========== 비즈니스 로직 ==========

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
