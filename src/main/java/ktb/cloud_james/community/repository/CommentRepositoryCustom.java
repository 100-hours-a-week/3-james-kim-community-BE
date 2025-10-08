package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.dto.comment.CommentListResponseDto;

import java.util.List;

/**
 * Comment 커스텀 Repository
 * - QueryDSL을 활용한 복잡한 쿼리
 */
public interface CommentRepositoryCustom {

    /**
     * 댓글 목록 조회 (인피니티 스크롤)
     * @param postId 게시글 ID
     * @param lastSeenId 마지막으로 본 댓글 ID (커서)
     * @param limit 페이지당 댓글 수
     * @param currentUserId 현재 로그인한 사용자 ID (작성자 여부 확인용)
     */
    List<CommentListResponseDto.CommentSummaryDto> findCommentsWithCursor(
            Long postId,
            Long lastSeenId,
            int limit,
            Long currentUserId
    );
}
