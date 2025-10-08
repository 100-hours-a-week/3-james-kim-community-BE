package ktb.cloud_james.community.service;


import ktb.cloud_james.community.dto.post.PostCreateRequestDto;
import ktb.cloud_james.community.dto.post.PostCreateResponseDto;
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
}
