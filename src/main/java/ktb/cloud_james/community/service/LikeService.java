package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.like.LikeResponseDto;
import ktb.cloud_james.community.entity.Post;
import ktb.cloud_james.community.entity.PostLike;
import ktb.cloud_james.community.entity.PostStats;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.repository.PostLikeRepository;
import ktb.cloud_james.community.repository.PostRepository;
import ktb.cloud_james.community.repository.PostStatsRepository;
import ktb.cloud_james.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 관련 비즈니스 로직
 * - 좋아요 추가/취소
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요 처리 흐름:
     * 1. 게시글 존재 확인 (삭제된 게시글 체크)
     * 2. 사용자 조회
     * 3. 기존 좋아요 여부 확인
     * 4-A. 이미 좋아요 누름 → 취소 (Hard Delete + likeCount -1)
     * 4-B. 좋아요 안 누름 → 추가 (INSERT + likeCount +1)
     * 5. 응답 DTO 생성
     */
    @Transactional
    public LikeResponseDto like(Long userId, Long postId) {
        log.info("좋아요 시도 - userId: {}, postId: {}", userId, postId);

        // 1. 게시글 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("좋아요 실패 - 존재하지 않는 게시글: postId={}", postId);
                    return new CustomException(ErrorCode.POST_NOT_FOUND);
                });

        // 1-2. 삭제된 게시글 체크
        if (post.getDeletedAt() != null) {
            log.warn("좋아요 실패 - 삭제된 게시글: postId={}", postId);
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("좋아요 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 3. 기존 좋아요 여부 확인
        PostLike existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .orElse(null);

        boolean isLiked;

        if (existingLike != null) {
            // 4-A. 이미 좋아요 누름 → 취소 (Hard Delete)
            postLikeRepository.delete(existingLike);
            log.info("좋아요 취소 (Hard Delete) - likeId: {}, userId: {}, postId: {}",
                    existingLike.getId(), userId, postId);

            // 좋아요 수 감소
            int updated = postStatsRepository.decrementLikeCount(postId);
            if (updated == 0) {
                log.error("좋아요 수 감소 실패 - PostStats 없거나 이미 0: postId={}", postId);
            }

            isLiked = false;

        } else {
            // 4-B. 좋아요 안 누름 → 추가 (INSERT)
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();

            postLikeRepository.save(newLike);
            log.info("좋아요 추가 - likeId: {}, userId: {}, postId: {}",
                    newLike.getId(), userId, postId);

            // 좋아요 수 증가
            int updated = postStatsRepository.incrementLikeCount(postId);
            if (updated == 0) {
                log.error("좋아요 수 증가 실패 - PostStats 없음: postId={}", postId);
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            isLiked = true;
        }

        // 5. 응답 DTO 생성(최신 좋아요 수 조회)
        Long currentLikeCount = postStatsRepository.findById(postId)
                .map(PostStats::getLikeCount)
                .orElse(0L);

        log.info("좋아요 완료 - userId: {}, postId: {}, isLiked: {}, likeCount: {}",
                userId, postId, isLiked, currentLikeCount);

        return new LikeResponseDto(isLiked, currentLikeCount);
    }
}
