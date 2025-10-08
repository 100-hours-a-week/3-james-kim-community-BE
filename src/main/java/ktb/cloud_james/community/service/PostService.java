package ktb.cloud_james.community.service;


import ktb.cloud_james.community.dto.post.PostCreateRequestDto;
import ktb.cloud_james.community.dto.post.PostCreateResponseDto;
import ktb.cloud_james.community.dto.post.PostDetailResponseDto;
import ktb.cloud_james.community.dto.post.PostListResponseDto;
import ktb.cloud_james.community.entity.Post;
import ktb.cloud_james.community.entity.PostImage;
import ktb.cloud_james.community.entity.PostStats;
import ktb.cloud_james.community.entity.User;
import ktb.cloud_james.community.global.exception.CustomException;
import ktb.cloud_james.community.global.exception.ErrorCode;
import ktb.cloud_james.community.repository.PostImageRepository;
import ktb.cloud_james.community.repository.PostRepository;
import ktb.cloud_james.community.repository.PostStatsRepository;
import ktb.cloud_james.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ViewCountCacheService viewCountCacheService;

    private static final int DEFAULT_PAGE_SIZE = 20; // 최초 기본 페이지 크기

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

        // 페이지 크기 설정 (null이면 기본값)
        int pageSize = (limit != null && limit > 0) ? limit : DEFAULT_PAGE_SIZE;

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
}
