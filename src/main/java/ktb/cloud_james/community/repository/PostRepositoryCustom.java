package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.dto.post.PostDetailResponseDto;
import ktb.cloud_james.community.dto.post.PostListResponseDto;

import java.util.List;
import java.util.Optional;

/**
 * Post 커스텀 Repository
 * - QueryDSL을 활용한 복잡한 쿼리
 */
public interface PostRepositoryCustom {

    /**
     * 게시글 목록 조회 (인피니티 스크롤)
     * @param lastSeenId 마지막으로 본 게시글 ID (커서)
     * @param limit 페이지당 게시글 수
     * @param currentUserId 현재 로그인한 사용자 ID (좋아요 여부 확인용)
     * @return 게시글 목록
     */
    List<PostListResponseDto.PostSummaryDto> findPostsWithCursor(
            Long lastSeenId,
            int limit,
            Long currentUserId
    );

    // 게시글 상세 조회 (단일 게시글)
    Optional<PostDetailResponseDto> findPostDetail(Long postId, Long currentUserId);
}
