package ktb.cloud_james.community.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ktb.cloud_james.community.dto.post.PostListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ktb.cloud_james.community.entity.QPost.post;
import static ktb.cloud_james.community.entity.QPostStats.postStats;
import static ktb.cloud_james.community.entity.QUser.user;
import static ktb.cloud_james.community.entity.QPostLike.postLike;

/**
 * Post 커스텀 Repository 구현체
 * - N+1 문제 해결: Fetch Join 활용
 * - 커서 기반 페이징
 */
@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PostListResponseDto.PostSummaryDto> findPostsWithCursor(
            Long lastSeenId,
            int limit,
            Long currentUserId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PostListResponseDto.PostSummaryDto.class,
                        post.id,
                        post.title,
                        user.nickname,
                        user.imageUrl,
                        post.createdAt,
                        postStats.likeCount,
                        postStats.commentCount,
                        postStats.viewCount,
                        isLikedByUser(currentUserId)  // 좋아요 여부
                ))
                .from(post)
                .join(post.user, user)                            // 작성자 정보 JOIN
                .join(postStats).on(postStats.postId.eq(post.id)) // 통계 정보 JOIN
                .where(
                        post.deletedAt.isNull(),    // Soft Delete 미적용 게시글만
                        cursorCondition(lastSeenId) // 커서 조건
                )
                .orderBy(post.id.desc())        // 최신순 (ID 역순)
                .limit(limit + 1)               // hasNext 판별용 +1
                .fetch();
    }

    /**
     * 커서 조건
     * 커서 기반 페이징 동작 원리:
     * - lastSeenId == null: 첫 페이지 조회 (모든 게시글 대상)
     * - lastSeenId != null: 다음 페이지 조회 (lastSeenId보다 작은 ID만 조회)
     *
     * 예시:
     * - 1페이지: WHERE deleted_at IS NULL ORDER BY id DESC LIMIT 6
     *   → 결과: [20, 19, 18, 17, 16] + hasNext(15 존재)
     *
     * - 2페이지: WHERE deleted_at IS NULL AND id < 16 ORDER BY id DESC LIMIT 6
     *   → 결과: [15, 14, 13, 12, 11] + hasNext(10 존재)
     */
    private BooleanExpression cursorCondition(Long lastSeenId) {
        return lastSeenId != null ? post.id.lt(lastSeenId) : null;
    }

    /**
     * 현재 사용자의 좋아요 여부
     */
    private BooleanExpression isLikedByUser(Long userId) {
        return queryFactory
                .selectOne()
                .from(postLike)
                .where(
                        postLike.post.id.eq(post.id),
                        postLike.user.id.eq(userId)
                )
                .exists();
    }
}
