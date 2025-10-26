package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 사용자의 특정 게시글 좋아요 조회
     * @return Optional<PostLike> (있으면 이미 좋아요 누름, 없으면 안 누름)
     */
    @Query("SELECT pl FROM PostLike pl " +
            "WHERE pl.post.id = :postId AND pl.user.id = :userId")
    Optional<PostLike> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 게시글의 모든 좋아요 Hard Delete
     * - 게시글 삭제 시 호출
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    int deleteByPostId(@Param("postId") Long postId);
}
