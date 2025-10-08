package ktb.cloud_james.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용 기본 생성자 자동 생성
public class PostStats {

    /**
     * @Id만 있고 @GeneratedValue 없음
     * 왜? 이 엔티티의 PK는 직접 할당되기 때문 -> Post의 ID를 그대로 가져다 쓸 것임
     */
    @Id
    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    /**
     * @MapsId: 이 관계의 FK를 내 PK로 사용하겠다
     * PostStats의 postId = Post의 id 값이 자동으로 들어감
     * @OneToOne: 1:1 관계 (Post 하나 -> PostStats 하나)
     *
     * 동작 원리:
     * 1. Post post = new Post(...);
     * 2. postRepository.save(post); // post.id = 1
     * 3. PostStats stats = new PostStats(post);
     * 4. postStatsRepository.save(stats); // stats.postId = 1 (자동)
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    /**
     * 조회수는 BIGINT UNSIGNED (Long)
     * 좋아요/댓글수는 INT UNSIGNED (Integer)
     * 기본값 = 0: 객체 생성 시 0으로 초기화
     */
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    /**
     * Builder 없이 생성자만 제공
     * PostStats는 Post와 함께 생성되어야 하므로 Post만 받는 단순 생성자로 충분
     */
    public PostStats(Post post) {
        this.post = post;
    }
}
