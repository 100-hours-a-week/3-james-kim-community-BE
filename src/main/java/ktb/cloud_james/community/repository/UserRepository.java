package ktb.cloud_james.community.repository;

import ktb.cloud_james.community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== 회원가입 관련 ==========
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
