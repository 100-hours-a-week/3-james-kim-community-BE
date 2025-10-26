package ktb.cloud_james.community.service;

import jakarta.annotation.PreDestroy;
import ktb.cloud_james.community.repository.PostStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 조회수 캐시 관리 서비스
 *
 * 전략:
 * 1. 인메모리 캐시 (ConcurrentHashMap<postId, AtomicLong>)
 * 2. 조회 시마다 캐시 증가 (DB 업데이트 X)
 * 3. 스케줄러로 주기적 DB 동기화 (30초마다)
 *  - 주기는 아마 데이터 테스트 받아보고 조정해야할 듯 싶습니다. 기본적으로 30초 정도면 괜찮을 것 같아 설정
 *
 * 장점:
 * - DB 부하 감소 (30초에 1번만 UPDATE)
 * - 동시성 안전 (AtomicLong)
 *
 * 단점: (현재는 단일 DB + 단일 서버, 조회수는 크게 중요한 실시간성 데이터는 아니어서 단점 상쇄 가능)
 * - 서버 재시작 시 동기화 전 데이터 손실 가능
 * - 멀티 서버 환경에서는 각 서버마다 별도 캐시 (향후 Redis 같은거 활용 한다면 전환 필요)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewCountCacheService {

    private final PostStatsRepository postStatsRepository;

    /**
     * 조회수 캐시
     * - Key: postId
     * - Value: 누적 조회수 (AtomicLong으로 동시성 보장)
     */
    private final ConcurrentHashMap<Long, AtomicLong> viewCountCache = new ConcurrentHashMap<>();

    // 조회수 증가 (인메모리 캐시만 업데이트)
    public Long incrementViewCount(Long postId) {
        // computeIfAbsent: postId가 없으면 새로 생성 (AtomicLong(0))
        AtomicLong count = viewCountCache.computeIfAbsent(
                postId,
                k -> new AtomicLong(0)
        );

        // 원자적 1 증가
        long newCount = count.incrementAndGet();

        log.debug("조회수 증가 (캐시) - postId: {}, count: {}", postId, newCount);

        return newCount;
    }

    /**
     * 캐시된 조회수 조회(DB 반영 전)
     */
    public Long getCachedViewCount(Long postId) {
        AtomicLong count = viewCountCache.get(postId);
        return count != null ? count.get() : 0L;
    }

    /**
     * 주기적 DB 동기화 (30초마다 실행)
     *
     * @Scheduled(fixedDelay = 30_000)
     * - 이전 작업 종료 후 30초 대기
     * - cron보다 안전 (이전 작업이 길어져도 중복 실행 방지)
     */
    @Scheduled(fixedDelay = 30_000)  // 30,000ms
    @Transactional
    public void syncViewCountsToDB() {
        if (viewCountCache.isEmpty()) {
            log.debug("동기화할 조회수 없음");
            return;
        }

        log.info("========== 조회수 DB 동기화 시작 ==========");
        log.info("동기화 대상 게시글 수: {}", viewCountCache.size());

        int successCount = 0;
        int failCount = 0;

        // 캐시 순회하며 DB 업데이트
        for (Map.Entry<Long, AtomicLong> entry : viewCountCache.entrySet()) {
            Long postId = entry.getKey();
            Long cachedCount = entry.getValue().get();

            try {
                // DB에 누적 조회수 추가
                int updated = postStatsRepository.incrementViewCount(postId, cachedCount);

                if (updated > 0) {
                    successCount++;
                    log.debug("조회수 동기화 성공 - postId: {}, count: +{}", postId, cachedCount);
                } else {
                    failCount++;
                    log.warn("조회수 동기화 실패 - postId: {} (게시글 없음)", postId);
                }

            } catch (Exception e) {
                failCount++;
                log.error("조회수 동기화 예외 - postId: {}", postId, e);
            }
        }

        // 동기화 완료 후 캐시 초기화
        viewCountCache.clear();

        log.info("========== 조회수 DB 동기화 완료 ==========");
        log.info("성공: {}건, 실패: {}건", successCount, failCount);
    }

    /**
     * 서버 종료 시 강제 동기화 (Spring의 @PreDestroy)
     * - 서버 재시작 시 데이터 손실 최소화
     */
    @PreDestroy
    public void shutdownHook() {
        log.warn("========== 서버 종료 감지: 조회수 강제 동기화 ==========");
        syncViewCountsToDB();
    }
}
