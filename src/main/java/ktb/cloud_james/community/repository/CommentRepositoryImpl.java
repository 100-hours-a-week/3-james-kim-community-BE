package ktb.cloud_james.community.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ktb.cloud_james.community.dto.comment.CommentListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ktb.cloud_james.community.entity.QComment.comment;
import static ktb.cloud_james.community.entity.QUser.user;

/**
 * Comment 커스텀 Repository 구현체
 * - N+1 문제 해결: JOIN 활용
 * - 커서 기반 페이징
 */
@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    /**
     * 댓글 목록 조회 (인피니티 스크롤)
     * 1. 특정 게시글(postId)의 댓글만 조회
     * 2. Soft Delete되지 않은 댓글만 조회
     * 3. 작성자 정보 JOIN으로 한 번에 가져오기 (N+1 방지)
     * 4. 커서 기반 페이징 (lastSeenId보다 작은 ID만 조회)
     * 5. 최신순 정렬 (ID 역순)
     */
    @Override
    public List<CommentListResponseDto.CommentSummaryDto> findCommentsWithCursor(
            Long postId,
            Long lastSeenId,
            int limit,
            Long currentUserId
    ) {

        return queryFactory
                .select(Projections.constructor(
                        CommentListResponseDto.CommentSummaryDto.class,
                        comment.id,
                        comment.content,
                        user.nickname,
                        user.imageUrl,
                        user.deletedAt.isNotNull(), // 탈퇴 여부 추가
                        comment.createdAt,
                        comment.user.id.eq(currentUserId)
                ))
                .from(comment)
                .join(comment.user, user)
                .where(
                        comment.post.id.eq(postId),
                        comment.deletedAt.isNull(),
                        cursorCondition(lastSeenId)
                )
                .orderBy(comment.id.desc())
                .limit(limit + 1)
                .fetch();
    }

    /**
     * 커서 조건
     * 커서 기반 페이징 동작 원리:
     * - lastSeenId == null: 첫 페이지 조회 (모든 댓글 대상)
     * - lastSeenId != null: 다음 페이지 조회 (lastSeenId보다 작은 ID만)
     *
     * 예시:
     * - 1페이지: WHERE post_id=1 AND deleted_at IS NULL ORDER BY id DESC LIMIT 11
     *   → 결과: [50, 49, 48, 47, 46, 45, 44, 43, 42, 41] + hasNext(40 존재)
     *
     * - 2페이지: WHERE post_id=1 AND deleted_at IS NULL AND id < 41 ORDER BY id DESC LIMIT 11
     *   → 결과: [40, 39, 38, 37, 36, 35, 34, 33, 32, 31] + hasNext(30 존재)
     */
    private BooleanExpression cursorCondition(Long lastSeenId) {
        return lastSeenId != null ? comment.id.lt(lastSeenId) : null;
    }
}
