package ktb.cloud_james.community.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ktb.cloud_james.community.dto.post.PostDetailResponseDto;
import ktb.cloud_james.community.dto.post.PostListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ktb.cloud_james.community.entity.QPost.post;
import static ktb.cloud_james.community.entity.QPostStats.postStats;
import static ktb.cloud_james.community.entity.QUser.user;
import static ktb.cloud_james.community.entity.QPostImage.postImage;
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

    /**
     * 게시글 목록 조회 (인피니티 스크롤)
     * - 탈퇴 여부 포함하여 조회 (회원탈퇴까지 구현 후 수정)
     */
    @Override
    public List<PostListResponseDto.PostSummaryDto> findPostsWithCursor(
            Long lastSeenId,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PostListResponseDto.PostSummaryDto.class,
                        post.id,
                        post.title,
                        user.nickname,
                        user.imageUrl,
                        user.deletedAt.isNotNull(), // 탈퇴 여부 추가
                        post.createdAt,
                        postStats.likeCount,
                        postStats.commentCount,
                        postStats.viewCount
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
     * 게시글 상세 조회 특징:
     * - 단일 쿼리로 모든 데이터 조회 (N+1 방지)
     * - 작성자, 통계, 이미지, 좋아요 여부 모두 JOIN
     * - 조회수는 캐시 값 사용 (DB 값 + 캐시 값)
     */
    @Override
    public Optional<PostDetailResponseDto> findPostDetail(Long postId, Long currentUserId) {
        PostDetailResponseDto result = queryFactory
                .select(Projections.constructor(
                        PostDetailResponseDto.class,
                        post.id,
                        post.title,
                        post.content,
                        getMainImageUrl(),
                        post.createdAt,
                        post.updatedAt,
                        Projections.constructor( // 작성자 정보
                                PostDetailResponseDto.AuthorInfo.class,
                                user.nickname,
                                user.imageUrl,
                                user.deletedAt.isNotNull() // 탈퇴 여부 추가
                        ),
                        Projections.constructor( // 통계 정보
                                PostDetailResponseDto.StatsInfo.class,
                                postStats.likeCount,
                                postStats.commentCount,
                                postStats.viewCount
                        ),
                        isLikedByUser(currentUserId),
                        post.user.id.eq(currentUserId)
                ))
                .from(post)
                .join(post.user, user)
                .join(postStats).on(postStats.postId.eq(post.id))
                .where(
                        post.id.eq(postId),
                        post.deletedAt.isNull()  // 삭제된 게시글 제외
                )
                .fetchOne();  // 단일 결과 (없으면 null)

        return Optional.ofNullable(result);
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

    /**
     * 메인 이미지 URL 조회
     */
    private Expression<String> getMainImageUrl() {
        return queryFactory
                .select(postImage.imageUrl)
                .from(postImage)
                .where(
                        postImage.post.id.eq(post.id),
                        postImage.isMain.isTrue(),
                        postImage.deletedAt.isNull()
                )
                .limit(1);
    }
}
