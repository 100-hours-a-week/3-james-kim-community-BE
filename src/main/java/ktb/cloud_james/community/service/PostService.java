package ktb.cloud_james.community.service;


import ktb.cloud_james.community.dto.post.*;
import ktb.cloud_james.community.entity.Post;
import ktb.cloud_james.community.entity.PostImage;
import ktb.cloud_james.community.entity.PostStats;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 관련 비즈니스 로직
 * - 게시글 작성, 수정, 삭제, 조회 등
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ViewCountCacheService viewCountCacheService;

    private static final int DEFAULT_PAGE_SIZE = 20; // 최초 기본 페이지 크기
    private static final int MAX_PAGE_SIZE = 50;     // 잘못된 페이지 크게 들어올 것 방지

    /**
     * 게시글 작성 처리 흐름:
     * 1. 사용자 조회
     * 2. 임시 이미지 → 정식 디렉토리 이동 (있을 경우)
     * 3. Post 엔티티 생성 및 저장
     * 4. PostStats 생성 및 저장
     * 5. PostImage 생성 및 저장 (이미지 있을 경우)
     * 6. 실패 시 이미지 롤백
     */
    @Transactional
    public PostCreateResponseDto createPost(Long userId, PostCreateRequestDto request) {
        log.info("게시글 작성 시도 - userId: {}, title: {}", userId, request.getTitle());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("게시글 작성 실패 - 존재하지 않는 사용자: userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
        });

        // 2. 임시 이미지 → 정식 디렉토리 이동
        String finalImageUrl = null;
        String tempImageUrl = request.getImageUrl();

        if (tempImageUrl != null && tempImageUrl.startsWith("/temp/")) {
            try {
                finalImageUrl = imageService.moveToPermanent(tempImageUrl);
                log.info("게시글 이미지 이동 완료 - {} → {}", tempImageUrl, finalImageUrl);
            } catch (Exception e) {
                log.error("게시글 이미지 이동 실패 - tempUrl: {}", tempImageUrl, e);
                throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }

        try {
            // 3. Post 엔티티 생성 및 저장
            Post post = Post.builder()
                    .user(user)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .build();

            Post savedPost = postRepository.save(post);
            log.info("게시글 저장 완료 - postId: {}", savedPost.getId());

            // 4. PostStats 생성 및 저장
            PostStats postStats = new PostStats(savedPost);
            postStatsRepository.save(postStats);
            log.info("게시글 통계 초기화 완료 - postId: {}", savedPost.getId());

            // 5. PostImage 생성 및 저장 (이미지가 있는 경우에만)
            if (finalImageUrl != null) {
                PostImage postImage = PostImage.builder()
                        .post(savedPost)
                        .imageUrl(finalImageUrl)
                        .imageOrder(0)
                        .isMain(true) // 현재는 이미지 1개만 지원해서! -> 다중지원일 경우 로직 변화 필요
                        .build();

                postImageRepository.save(postImage);
                log.info("게시글 이미지 저장 완료 - postId: {}, imageUrl: {}", savedPost.getId(), finalImageUrl);
            }

            return new PostCreateResponseDto(savedPost.getId());
        } catch (Exception e) {
            // DB 저장 실패 시 이미지 삭제 (Best Effort)
            if (finalImageUrl != null) {
                imageService.deleteFile(finalImageUrl);
                log.error("게시글 작성 실패로 이미지 삭제 - imageUrl: {}", finalImageUrl);
            }
            throw e;
        }
    }

    /**
     * 게시글 목록 조회 (인피니티 스크롤)
     */
    public PostListResponseDto getPostList(Long lastSeenId, Integer limit, Long currentUserId) {
        log.info("게시글 목록 조회 - lastSeenId: {}, limit: {}, userId: {}",
                lastSeenId, limit, currentUserId);

        int pageSize = DEFAULT_PAGE_SIZE;
        if (limit != null) {
            if (limit > MAX_PAGE_SIZE) limit = MAX_PAGE_SIZE;
            if (limit > 0) pageSize = limit;
        }

        // 게시글 조회 (limit + 1개 조회하여 hasNext 판별)
        List<PostListResponseDto.PostSummaryDto> posts =
                postRepository.findPostsWithCursor(lastSeenId, pageSize, currentUserId);

        // hasNext 판별
        boolean hasNext = posts.size() > pageSize;
        if (hasNext) {
            posts = posts.subList(0, pageSize);
        }

        // 다음 커서 값 (마지막 게시글 ID)
        Long nextCursor = posts.isEmpty() ? null : posts.get(posts.size() - 1).getPostId();

        // 페이징 정보 생성
        PostListResponseDto.PaginationInfo pagination = PostListResponseDto.PaginationInfo.builder()
                .lastSeenId(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .sort("latest")
                .build();

        log.info("게시글 목록 조회 완료 - 조회된 게시글: {}개, hasNext: {}", posts.size(), hasNext);

        return PostListResponseDto.builder()
                .posts(posts)
                .pagination(pagination)
                .build();
    }

    /**
     * 게시글 상세 조회 처리 흐름:
     * 1. 게시글 조회
     * 2. 조회수 증가 (캐시만 업데이트)
     * 3. 캐시된 조회수를 응답에 반영
     */
    public PostDetailResponseDto getPostDetail(Long postId, Long currentUserId) {
        log.info("게시글 상세 조회 - postId: {}, userId: {}", postId, currentUserId);

        // 1. 게시글 조회
        PostDetailResponseDto post = postRepository.findPostDetail(postId, currentUserId)
                .orElseThrow(() -> {
                    log.warn("게시글 조회 실패 - postId: {} (존재하지 않거나 삭제됨)", postId);
                    return new CustomException(ErrorCode.POST_NOT_FOUND);
                });

        // 2. 조회수 증가 (인메모리 캐시만 업데이트, DB는 스케줄러가 동기화)
        Long cachedViewCount = viewCountCacheService.incrementViewCount(postId);

        // 3. 응답 DTO에 반영 (DB값 + 캐시 증가분)
        PostDetailResponseDto.StatsInfo updatedStats = PostDetailResponseDto.StatsInfo.builder()
                .likeCount(post.getStats().getLikeCount())
                .commentCount(post.getStats().getCommentCount())
                .viewCount(post.getStats().getViewCount() + cachedViewCount)
                .build();

        log.info("게시글 상세 조회 완료 - postId: {}, 조회수: {} (DB: {}, 캐시: +{})",
                postId,
                updatedStats.getViewCount(),
                post.getStats().getViewCount(),
                cachedViewCount);

        return PostDetailResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(post.getAuthor())
                .stats(updatedStats)
                .isLiked(post.getIsLiked())
                .isAuthor(post.getIsAuthor())
                .build();
    }

    /**
     * 게시글 수정 처리 흐름:
     * 1. 게시글 조회 및 권한 확인
     * 2. 수정 요청 검증 (최소 1개 필드는 수정되어야 함)
     * 3. 이미지 처리 (교체/삭제/유지)
     * 4. 게시글 업데이트
     * 5. 실패 시 롤백
     */
    @Transactional
    public PostUpdateResponseDto updatePost(Long userId, Long postId, PostUpdateRequestDto request) {
        log.info("게시글 수정 시도 - userId: {}, postId: {}", userId, postId);

        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("게시글 수정 실패 - 존재하지 않는 게시글: postId={}", postId);
                    return new CustomException(ErrorCode.POST_NOT_FOUND);
                });

        // 1-2. 작성자 권한 확인
        if (!post.getUser().getId().equals(userId)) {
            log.warn("게시글 수정 실패 - 권한 없음: userId={}, postId={}", userId, postId);
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // 2. 수정 요청 검증
        if (!request.hasAnyUpdate()) {
            log.warn("게시글 수정 실패 - 수정할 내용 없음: postId={}", postId);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 3. 이미지 처리
        String finalImageUrl = handleImageUpdate(post, request.getImageUrl());

        try {

            // 4. 게시글 업데이트 시도
            updatePostFields(post, request, finalImageUrl);

            log.info("게시글 수정 완료 - postId: {}", postId);

            return new PostUpdateResponseDto(postId);
        } catch (Exception e) {
            // 실패 시 새로 이동한 이미지 삭제
            if (finalImageUrl != null && finalImageUrl.startsWith("/images/")) {
                imageService.deleteFile(finalImageUrl);
                log.error("게시글 수정 실패로 이미지 삭제 - imageUrl: {}", finalImageUrl);
            }
            throw e;
        }
    }

    /**
     * 게시글 삭제 (Soft Delete) 처리 흐름:
     * 1. 게시글 조회 및 권한 확인
     * 2. Post Soft Delete (deleted_at 기록)
     * 3. PostImage Soft Delete
     * 4. Comments Soft Delete
     * 5. PostLike는 Hard Delete
     * 6. PostStats는 Hard Delete
     */

    @Transactional
    public void deletePost(Long userId, Long postId) {
        log.info("게시글 삭제 시도 - userId: {}, postId: {}", userId, postId);

        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("게시글 삭제 실패 - 존재하지 않는 게시글: postId={}", postId);
                    return new CustomException(ErrorCode.POST_NOT_FOUND);
                });


        // 1-2. 이미 삭제된 게시글인지 확인
        if (post.getDeletedAt() != null) {
            log.warn("게시글 삭제 실패 - 이미 삭제된 게시글: postId={}", postId);
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 1-3. 작성자 권한 확인
        if (!post.getUser().getId().equals(userId)) {
            log.warn("게시글 삭제 실패 - 권한 없음: userId={}, postId={}", userId, postId);
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        // 2. 게시글 삭제
        post.softDelete();
        log.info("게시글 Soft Delete 완료 - postId: {}", postId);

        // 3. 게시글 이미지 삭제 (있는 경우에만)
        int deletedImages = postImageRepository.softDeleteByPostId(postId, LocalDateTime.now());
        if (deletedImages > 0) {
            log.info("게시글 이미지 Soft Delete 완료 - postId: {}, 삭제된 이미지 수: {}",
                    postId, deletedImages);
        }

        // 4. 댓글 삭제 (있는 경우에만)
        int deletedComments = commentRepository.softDeleteByPostId(postId, LocalDateTime.now());
        if (deletedComments > 0) {
            log.info("게시글 댓글 Soft Delete 완료 - postId: {}, 삭제된 댓글 수: {}",
                    postId, deletedComments);
        }

        // 5. PostLike 삭제
        int deletedLikes = postLikeRepository.deleteByPostId(postId);
        if (deletedLikes > 0) {
            log.info("게시글 좋아요 Hard Delete 완료 - postId: {}, 삭제된 좋아요 수: {}",
                    postId, deletedLikes);
        }

        // 6. PostStats 삭제
        postStatsRepository.deleteById(postId);
        log.info("게시글 통계 Hard Delete 완료 - postId: {}", postId);

        log.info("게시글 삭제 완료 - postId: {}", postId);
    }

    private String handleImageUpdate(Post post, String requestImageUrl) {
        // null: 이미지 수정 안 함
        if (requestImageUrl == null) {
            log.debug("이미지 수정 없음 - postId: {}", post.getId());
            return null;
        }

        // 빈 문자열: 이미지 삭제
        if (requestImageUrl.isEmpty()) {
            log.info("이미지 삭제 요청 - postId: {}", post.getId());
            softDeletePostImage(post.getId());
            return ""; // 빈 문자열을 반환하여 삭제 표시
        }

        // 임시 이미지: 새 이미지로 교체
        if (requestImageUrl.startsWith("/temp/")) {
            log.info("이미지 교체 요청 - postId: {}, tempUrl: {}", post.getId(), requestImageUrl);

            // 기존 이미지 Soft Delete
            softDeletePostImage(post.getId());

            // 새 이미지를 정식 디렉토리로 이동
            try {
                String newImageUrl = imageService.moveToPermanent(requestImageUrl);
                log.info("이미지 이동 완료 - {} → {}", requestImageUrl, newImageUrl);
                return newImageUrl;
            } catch (Exception e) {
                log.error("이미지 이동 실패 - tempUrl: {}", requestImageUrl, e);
                throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }

        // 그 외의 경우: 무시
        log.warn("잘못된 이미지 URL - postId: {}, imageUrl: {}", post.getId(), requestImageUrl);
        throw new CustomException(ErrorCode.INVALID_REQUEST);
    }

    /**
     * 게시글 필드 업데이트
     * - JPA Dirty Checking 활용 (별도 save() 불필요)
     */
    private void updatePostFields(Post post, PostUpdateRequestDto request, String finalImageUrl) {
        // 제목 수정
        if (request.getTitle() != null) {
            post.updateTitle(request.getTitle());
            log.debug("제목 수정 - postId: {}, title: {}", post.getId(), request.getTitle());
        }

        // 내용 수정
        if (request.getContent() != null) {
            post.updateContent(request.getContent());
            log.debug("내용 수정 - postId: {}", post.getId());
        }

        // 이미지 처리
        if (finalImageUrl != null) {
            if (finalImageUrl.isEmpty()) {
                // 이미지 삭제됨 (이미 Soft Delete 완료)
                log.debug("이미지 삭제 완료 - postId: {}", post.getId());
            } else {
                // 새 이미지 저장
                PostImage newImage = PostImage.builder()
                        .post(post)
                        .imageUrl(finalImageUrl)
                        .imageOrder(0)
                        .isMain(true)
                        .build();
                postImageRepository.save(newImage);
                log.debug("새 이미지 저장 완료 - postId: {}, imageUrl: {}", post.getId(), finalImageUrl);
            }
        }
    }

    private void softDeletePostImage(Long postId) {
        int deleted = postImageRepository.softDeleteByPostId(postId, LocalDateTime.now());
        if (deleted > 0) {
            log.info("이미지 Soft Delete 완료 - postId: {}", postId);
        }
    }
}
