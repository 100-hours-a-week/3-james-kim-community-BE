package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.entity.Post;
import ktb.cloud_james.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /**
     * 특정 게시글의 메인 이미지 조회
     * - 현재는 이미지 1개만 지원하므로 is_main=true인 것 조회
     */
    Optional<PostImage> findByPostAndIsMain(Post post, Boolean isMain);
}
