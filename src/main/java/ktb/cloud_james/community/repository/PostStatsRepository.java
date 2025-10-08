package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.entity.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// PostStats의 PK는 post_id (Post의 PK와 동일)
@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, Long> {

}
