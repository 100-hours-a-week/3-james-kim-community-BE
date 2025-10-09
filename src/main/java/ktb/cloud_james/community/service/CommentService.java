package ktb.cloud_james.community.service;

import ktb.cloud_james.community.dto.comment.*;
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

import java.util.List;

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

    private static final int DEFAULT_COMMENT_PAGE_SIZE = 10;
    private static final int MAX_COMMENT_PAGE_SIZE = 30;

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
        if (post.isDeleted()) {
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

    /**
     * 댓글 목록 조회 (인피니티 스크롤)
     * - 탈퇴한 회원 후처리 로직 추가
     */
    public CommentListResponseDto getCommentList(
            Long postId,
            Long lastSeenId,
            Integer limit,
            Long currentUserId
    ) {
        log.info("댓글 목록 조회 - postId: {}, lastSeenId: {}, limit: {}, userId: {}",
                postId, lastSeenId, limit, currentUserId);

        // 게시글 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("댓글 조회 실패 - 존재하지 않는 게시글: postId={}", postId);
                    return new CustomException(ErrorCode.POST_NOT_FOUND);
                });

        if (post.isDeleted()) {
            log.warn("댓글 조회 실패 - 삭제된 게시글: postId={}", postId);
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 페이지 크기 설정
        int pageSize = DEFAULT_COMMENT_PAGE_SIZE;
        if (limit != null) {
            if (limit > MAX_COMMENT_PAGE_SIZE) limit = MAX_COMMENT_PAGE_SIZE;
            if (limit > 0) pageSize = limit;
        }

        // 댓글 조회 (limit + 1개 조회하여 hasNext 판별)
        List<CommentListResponseDto.CommentSummaryDto> comments =
                commentRepository.findCommentsWithCursor(postId, lastSeenId, pageSize, currentUserId);

        // 탈퇴한 회원 마스킹 처리
        comments.forEach(CommentListResponseDto.CommentSummaryDto::maskDeletedUser);

        // hasNext 판별
        boolean hasNext = comments.size() > pageSize;
        if (hasNext) {
            comments = comments.subList(0, pageSize);
        }

        // 다음 커서 값 (마지막 댓글 ID)
        Long nextCursor = comments.isEmpty() ? null : comments.get(comments.size() - 1).getCommentId();

        // 페이징 정보 생성
        CommentListResponseDto.PaginationInfo pagination = CommentListResponseDto.PaginationInfo.builder()
                .lastSeenId(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();

        log.info("댓글 목록 조회 완료 - postId: {}, 조회된 댓글: {}개, hasNext: {}", postId, comments.size(), hasNext);

        return CommentListResponseDto.builder()
                .comments(comments)
                .pagination(pagination)
                .build();
    }

    /**
     * 댓글 수정 처리 흐름:
     * 1. 댓글 조회 (존재 확인)
     * 2. 삭제된 댓글 체크
     * 3. 작성자 권한 확인
     * 4. 댓글 내용 수정 (JPA Dirty Checking)
     * 5. 응답 DTO 생성
     */
    @Transactional
    public CommentUpdateResponseDto updateComment(
            Long userId,
            Long postId,
            Long commentId,
            CommentUpdateRequestDto request
    ) {
        log.info("댓글 수정 시도 - userId: {}, postId: {}, commentId: {}", userId, postId, commentId);

        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("댓글 수정 실패 - 존재하지 않는 댓글: commentId={}", commentId);
                    return new CustomException(ErrorCode.COMMENT_NOT_FOUND);
                });

        // 2. 삭제된 댓글 체크
        if (comment.isDeleted()) {
            log.warn("댓글 수정 실패 - 삭제된 댓글: commentId={}", commentId);
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 3. 게시글 ID 일치 확인 (URL의 postId와 댓글의 postId 비교)
        if (!comment.belongsToPost(postId)) {
            log.warn("댓글 수정 실패 - 게시글 불일치: commentId={}, urlPostId={}, actualPostId={}",
                    commentId, postId, comment.getPost().getId());
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 4. 작성자 권한 확인
        if (!comment.isAuthor(userId)) {
            log.warn("댓글 수정 실패 - 권한 없음: userId={}, commentId={}, authorId={}",
                    userId, commentId, comment.getUser().getId());
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        // 5. 댓글 내용 수정 (JPA Dirty Checking으로 자동 UPDATE)
        comment.updateContent(request.getContent());

        log.info("댓글 수정 완료 - commentId: {}", commentId);

        return new CommentUpdateResponseDto(commentId);
    }

    /**
     * 댓글 삭제 처리 흐름 (Soft Delete):
     * 1. 댓글 조회
     * 2. 이미 삭제된 댓글 체크
     * 3. 게시글/작성자 권한 확인
     * 4. 댓글 Soft Delete (deleted_at 기록)
     * 5. PostStats의 댓글 수 감소 (원자적 연산)
     * 6. 응답 DTO 생성
     */
    @Transactional
    public CommentDeleteResponseDto deleteComment(
            Long userId,
            Long postId,
            Long commentId
    ) {
        log.info("댓글 삭제 시도 - userId: {}, postId: {}, commentId: {}",
                userId, postId, commentId);

        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("댓글 삭제 실패 - 존재하지 않는 댓글: commentId={}", commentId);
                    return new CustomException(ErrorCode.COMMENT_NOT_FOUND);
                });

        // 2. 이미 삭제된 댓글 체크
        if (comment.isDeleted()) {
            log.warn("댓글 삭제 실패 - 이미 삭제된 댓글: commentId={}", commentId);
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 3. 게시글 ID 일치 확인 (URL의 postId와 댓글의 postId 비교)
        if (!comment.belongsToPost(postId)) {
            log.warn("댓글 삭제 실패 - 게시글 불일치: commentId={}, urlPostId={}, actualPostId={}",
                    commentId, postId, comment.getPost().getId());
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 3-2. 작성자 권한 확인
        if (!comment.isAuthor(userId)) {
            log.warn("댓글 삭제 실패 - 권한 없음: userId={}, commentId={}, authorId={}",
                    userId, commentId, comment.getUser().getId());
            throw new CustomException(ErrorCode.NOT_COMMENT_AUTHOR);
        }

        // 4. 댓글 Soft Delete
        comment.softDelete();
        log.info("댓글 Soft Delete 완료 - commentId: {}", commentId);

        // 5. PostStats의 댓글 수 감소 (원자적 연산)
        int updated = postStatsRepository.decrementCommentCount(postId);
        if (updated == 0) {
            log.error("댓글 수 감소 실패 - PostStats 없거나 이미 0: postId={}", postId);
            // 댓글 수가 0이면 감소 안 함 (음수 방지)
            // 에러는 던지지 않고 경고만 로그
        }

        // 6. 응답 DTO 생성 (최신 댓글 수 조회)
        Long currentCommentCount = postStatsRepository.findById(postId)
                .map(PostStats::getCommentCount)
                .orElse(0L);

        log.info("댓글 삭제 완료 - commentId: {}, postId: {}, 댓글 수: {}",
                commentId, postId, currentCommentCount);

        return new CommentDeleteResponseDto(currentCommentCount);
    }
}
