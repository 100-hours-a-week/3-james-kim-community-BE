package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.comment.CommentCreateRequestDto;
import ktb.cloud_james.community.dto.comment.CommentCreateResponseDto;
import ktb.cloud_james.community.entity.Comment;
import ktb.cloud_james.community.entity.Post;
import ktb.cloud_james.community.entity.PostStats;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.repository.CommentRepository;
import ktb.cloud_james.community.repository.PostRepository;
import ktb.cloud_james.community.repository.PostStatsRepository;
import ktb.cloud_james.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 관련 비즈니스 로직
 * - 댓글 작성, 수정, 삭제, 조회 등
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 작성 처리 흐름:
     * 1. 게시글 존재 확인
     * 2. 사용자 조회
     * 3. Comment 엔티티 생성 및 저장
     * 4. PostStats의 댓글 수 증가 (원자적 연산)
     * 5. 응답 DTO 생성
     */
    @Transactional
    public CommentCreateResponseDto createComment(
            Long userId,
            Long postId,
            CommentCreateRequestDto request
    ) {
        log.info("댓글 작성 시도 - userId: {}, postId: {}", userId, postId);

        // 1. 게시글 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("댓글 작성 실패 - 존재하지 않는 게시글: postId={}", postId);
                    return new CustomException(ErrorCode.POST_NOT_FOUND);
                });

        // 1-2. 삭제된 게시글 체크
        if (post.getDeletedAt() != null) {
            log.warn("댓글 작성 실패 - 삭제된 게시글: postId={}", postId);
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("댓글 작성 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 3. Comment 엔티티 생성 및 저장
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 저장 완료 - commentId: {}, postId: {}", savedComment.getId(), postId);

        // 4. PostStats의 댓글 수 증가
        int updated = postStatsRepository.incrementCommentCount(postId);
        if (updated == 0) {
            log.error("댓글 수 증가 실패 - PostStats 없음: postId={}", postId);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 5. 최신 댓글 수 조회
        Long currentCommentCount = postStatsRepository.findById(postId)
                .map(PostStats::getCommentCount)
                .orElse(0L);

        log.info("댓글 작성 완료 - commentId: {}, postId: {}, 댓글 수: {}",
                savedComment.getId(), postId, currentCommentCount);

        // 6. 응답 DTO 생성
        return CommentCreateResponseDto.builder()
                .comment(CommentCreateResponseDto.CommentInfo.builder()
                        .commentId(savedComment.getId())
                        .content(savedComment.getContent())
                        .authorNickname(user.getNickname())
                        .authorProfileImage(user.getImageUrl())
                        .createdAt(savedComment.getCreatedAt())
                        .build())
                .commentsCount(currentCommentCount)
                .build();
    }
}
