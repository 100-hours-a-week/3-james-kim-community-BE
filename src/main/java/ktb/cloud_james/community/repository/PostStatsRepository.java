package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.entity.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// PostStats의 PK는 post_id (Post의 PK와 동일)
@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, Long> {

    // 조회수 증가 (원자적 연산)
    @Modifying
    @Query("UPDATE PostStats ps SET ps.viewCount = ps.viewCount + :count WHERE ps.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId, @Param("count") Long count);

}
