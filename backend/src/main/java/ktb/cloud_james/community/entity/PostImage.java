package ktb.cloud_james.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용 기본 생성자 자동 생성
public class PostImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL DB의 AUTO_INCREMENT 사용
    @Column(name = "image_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "image_order", nullable = false)
    private Integer imageOrder = 0;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false;

    // @CreationTimestamp: INSERT 시 현재 시간 자동 입력
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public PostImage(Post post, String imageUrl, String thumbnailUrl, Integer imageOrder, Boolean isMain) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.imageOrder = imageOrder;
        this.isMain = isMain;
    }
}
