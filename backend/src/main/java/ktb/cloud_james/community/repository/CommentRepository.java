package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    /**
     * 게시글의 모든 댓글 Soft Delete
     * - 게시글 삭제 시 호출
     * - deleted_at이 NULL인 댓글만 삭제 (이미 삭제된 댓글 제외)
     */
    @Modifying
    @Query("UPDATE Comment c SET c.deletedAt = :deletedAt " +
            "WHERE c.post.id = :postId AND c.deletedAt IS NULL")
    int softDeleteByPostId(@Param("postId") Long postId, @Param("deletedAt") LocalDateTime deletedAt);

}
