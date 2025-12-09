# 📌 프로젝트명: 와글와글 커뮤니티 (WagleWagle Community)
개인적인 고민부터 개발 이야기까지 자유롭게 소통할 수 있는 **커뮤니티 플랫폼의 백엔드 API 서버**입니다.  
초기 ERD설계부터 Spring Boot와 JPA를 기반으로 RESTful API를 설계하고, **JWT 인증/인가**, **QueryDSL 기반 성능 개선**, **AWS 인프라 구축**, **CI/CD 파이프라인 구성 및 무중단 배포**까지 전 과정을 직접 구현했습니다.  
또한, 대용량 트래픽(목표 MAU 100만+)까지 고려한 확장 가능한 아키텍처를 지향합니다.

---


## 📅 개발 인원 및 기간
- 개발 기간 : 2025-09 ~ 2025-11 
- 개발 인원 : 1인 (프론트엔드 & 백엔드 전체 개발)

<br/>
   
## 사용 기술 및 tools
### Backend

- **Framework:** Spring Boot 3.5
- **Language:** Java 21  
- **ORM:** JPA (Hibernate), QueryDSL  
- **Database:** MySQL 8.0  
- **Build Tool:** Gradle  

### Infrastructure & DevOps

- **Cloud:** AWS (VPC, ECS Fargate, ECR, ALB, Lambda, S3, RDS, CloudFront, CloudWatch)  
- **CI/CD:** GitHub Actions
- Docker, Nginx
     
- 서버 아키텍처
<img width="600" height="500" alt="스크린샷 2025-12-06 오후 10 32 57" src="https://github.com/user-attachments/assets/4e28d848-9caa-4634-81bc-2b2ad8be0d55" />


<br/>

## 관련 링크
- [프론트엔드 레포지토리](https://github.com/100-hours-a-week/3-james-kim-community-FE)
- [서비스 데모영상](https://drive.google.com/file/d/1QRmcLbqeb0l4wt5sW-tfE6V8EwNk_P0A/view?usp=sharing)

<br/>

---

## 폴더 구조
<details>
<summary>📁 폴더 구조 보기 / 접기</summary>
   
```bash
src/main/java/ktb/cloud_james/community/
├── controller/          # REST API 엔드포인트
│   ├── AuthController.java
│   ├── UserController.java
│   ├── PostController.java
│   ├── CommentController.java
│   ├── LikeController.java
│   ├── HealthController.java
│   └── PolicyController.java # SSR 이용약관, 개인정보 처리방침
│
├── service/            # 비즈니스 로직
│   ├── AuthService.java
│   ├── UserService.java
│   ├── PostService.java
│   ├── CommentService.java
│   ├── LikeService.java
│   └── ViewCountCacheService.java
│
├── repository/         # 데이터 접근 계층
│   ├── UserRepository.java
│   ├── UserTokenRepository.java
│   ├── PostRepository.java
│   ├── PostRepositoryCustom.java
│   ├── PostRepositoryImpl.java  # QueryDSL 구현체
│   ├── PostStatsRepository.java
│   ├── PostImageRepository.java
│   ├── PostLikeRepository.java
│   ├── CommentRepository.java
│   ├── CommentRepositoryCustom.java
│   └── CommentRepositoryImpl.java
│
├── entity/             # JPA 엔티티
│   ├── User.java
│   ├── UserToken.java
│   ├── Post.java
│   ├── PostStats.java
│   ├── PostImage.java
│   ├── PostLike.java
│   └── Comment.java
│
├── dto/                # 요청/응답 DTO
│   ├── auth/
│   ├── user/
│   ├── post/
│   ├── comment/
│   ├── like/
│   └── common/
│
└── global/             # 전역 설정 및 공통 모듈
    ├── config/
    ├── security/
    ├── exception/
    └── util/
```
</details>

---

## 주요 기능
### 1️⃣ 인증 & 보안
- JWT 기반 Stateless 이중 토큰 인증 시스템 구현
   - 적용 배경: 분산 환경에서의 확장성을 고려하여 세션 방식 대신 JWT 방식 채택
   - Spring Security + Custom Filter를 통한 요청 인증
   - Refresh Token 화이트리스트 관리로 탈취 리스크 최소화

### 2️⃣ 성능 최적화
- 조회수 캐싱 시스템 (In-Memory Cache)
   - 적용 배경: 게시글 조회 시마다 잦은 DB 업데이트로 인한 부하 발생
   - `ConcurrentHashMap` + `AtomicLong` 기반 Thread-safe 캐싱
- QueryDSL 기반 N+1 문제 해결
   - 적용 배경: JPA의 지연 로딩으로 인한 반복 쿼리 발생
   - 게시글/댓글 목록 조회 시 JOIN 최적화 (N+1 → 1 쿼리)
   - 게시글/댓글 Cursor 기반 무한 스크롤 페이징 구현

### 3️⃣ 인프라 & DevOps
- 컨테이너 기반 아키텍처
   - 적용 배경: EC2 기반 배포의 확장성 한계 및 운영 복잡도 증가
   - ECS Fargate + ALB를 통한 무중단 Rolling 배포
   - 트래픽 변화 유연한 대응을 위한 Auto Scaling 도입
   - Private Subnet 격리로 네트워크 보안 강화 (RDS, ECS(WAS))
- CI/CD 파이프라인 구축
   - 적용 배경: 배포 속도 개선 및 휴먼 에러 방지, 무중단 배포가 현대에는 거의 필수적
   - GitHub Actions 기반 빌드/테스트/배포 자동화
   - ECR 중앙 이미지 관리 및 버전별 롤백 지원

---

## 💡 기술적 도전과 해결 과정
### 1️⃣ 동시성 제어와 캐싱 전략
**문제 상황**   
게시글 조회 시마다 DB에 조회수 업데이트 쿼리를 실행하면서, 동시 접속자 증가 시 DB 병목 현상 발생   
   
**해결 과정**
- 1차 시도: Redis 분산 캐시 도입 검토
   - 초기 프로젝트 규모와 인프라 복잡도를 고려했을 때 과도한 엔지니어링으로 판단
   - 단일 서버 환경에서는 In-Memory 캐시만으로도 충분한 성능 개선 가능
- 최종 해결:
   - `ConcurrentHashMap<Long, AtomicLong>`으로 Thread-safe한 조회수 캐싱 구현
   - 스케줄러(`@Scheduled`)를 통해 30초마다 배치 DB 동기화
   - 서버 종료 시 `@PreDestroy`로 강제 동기화하여 데이터 손실 최소화
     
**기술적 근거**
- 조회수는 실시간 정합성이 크게 중요하지 않은 통계성 데이터
- 수십 초 단위의 지연은 사용자 경험에 큰 영향을 주지 않음
- 단일 서버 환경에서는 인메모리 캐시로도 동시성 제어 가능
- 향후 서버 증설 시에도 Redis로 전환이 용이한 구조 설계
   
**성과**   
- 게시글 조회 응답 속도 평균 30% 개선
- DB 커넥션 풀 사용률 감소로 전체 시스템 안정성 향상

**확장 가능성**
- 트래픽 증가 및 멀티 서버 환경으로 확장 시, 현재 ViewCountCacheService를 Redis 기반 구현체로 교체하는 것만으로 분산 캐싱 적용 가능
- 인터페이스 분리를 통해 확장성을 고려한 설계 완료

<br/>

### 2️⃣ JWT 인증 전략 (프론트엔드 + 백엔드)
**문제 상황**
프론트엔드가 Vanilla JS 기반 MPA 구조로, SPA처럼 메모리 상태 관리 불가. Access Token 저장 위치 및 자동 갱신 전략 필요
    
**해결 과정**
- 1차 시도: 메모리(JavaScript 변수)에 토큰 저장
   - MPA 특성상 페이지 이동 시마다 토큰 소실
   - 매번 로그인이 필요한 심각한 UX 저하 발생
- 2차 시도: 쿠키에 Access Token과 Refresh Token 모두 저장
   - CSRF(Cross-Site Request Forgery) 공격 취약점 존재
   - 두 토큰을 동일한 저장소에 보관하는 것은 보안 관점에서 부적절
- 최종 해결:
   - Access Token: `localStorage`에 저장하여 페이지 간 유지
   - Refresh Token: `HttpOnly` 쿠키에 저장 (JavaScript 접근 차단)
   - 페이지 로드 시 Refresh Token으로 Access Token 자동 재발급 로직 구현
   - Spring Security의 JwtAuthenticationFilter에서 토큰 검증 및 인증 처리
   
**기술적 근거**
- `sessionStorage`와 `localStorage` 모두 XSS 공격에 동일하게 노출되므로, 페이지 간 토큰 유지가 필요한 MPA 특성상 `localStorage` 선택이 합리적
- XSS 리스크 완화를 위해 Access Token 만료 시간을 일반적인 표준 시간보다 짧게 설정
- Refresh Token은 엄격한 화이트리스트 관리 및 쿠키 보안 옵션(HttpOnly, SameSite) 적용
- 이후 상황에 따라 보안 이슈가 크게 느껴진다면, RTR 패턴까지 가능성 열어둠
    
**확장 가능성**
- 세션 기반 인증도 고려했으나, 다음 이유로 JWT 방식을 최종 선택:
   - 모바일 확장성: 향후 앱 개발 시 세션보다 토큰 기반이 유리
   - 분산 환경 대응: 현대적인 MSA 구조로 확장 시 Stateless한 JWT가 서버 간 인증 공유에 적합
   - 서버 부하 감소: 세션 저장소 관리 부담 제거 및 수평 확장 용이

<br/>

### 3️⃣ EC2 → ECS Fargate 마이그레이션 (인프라 구조 발전)
**문제 상황**    
초기 EC2 기반 배포는 서버 관리 부담이 크고, Auto Scaling 설정이 복잡했음. 특히 트래픽 증가에 따라 컨테이너 인스턴스가 늘어날 경우 수동 관리의 한계가 명확
       
**해결 과정**
- Phase 1: Docker 컨테이너화
   - 개발/운영 환경 불일치 문제 해결
- Phase 2: Amazon ECR 도입
   - 컨테이너 이미지 중앙 집중 관리
   - 버전별 이미지 히스토리 관리 및 롤백 용이성 확보
- Phase 3: ECS Fargate로 전환
   - Task Definition: CPU, 메모리, 컨테이너 스펙을 코드로 정의
   - Service: Desired Count 기반 자동 복구 및 Rolling Update 설정
   - ALB 연동: Target Group을 통한 트래픽 분산 및 무중단 배포 구현

**마이그레이션 선택 근거**
1. 컨테이너 오케스트레이션 필요성   
- 초기에는 EC2 작은 규모의 ASG 정도로 충분하지만, 운영 환경에서는 확장성이 핵심
- EKS(Kubernetes)는 현재 프로젝트 규모 대비 과도한 복잡도
- 일반적인 성장 단계의 서비스는 ECS로 충분한 기능 제공
   
2. 비용 대비 효율성
- 직접 비용: Fargate는 EC2 대비 약 20-30% 서버 비용 증가
- 간접 비용: 컨테이너 관리, 서버 패치, 보안 업데이트, 모니터링 설정 등 운영 인력 비용 감소
- 최종 판단: 인프라 관리 부담 제거로 인한 개발 생산성 향상이 추가 비용 상쇄할 것
- ECS on EC2 vs Fargate: 비용은 절감되나 EC2 인스턴스 관리 부담은 여전히 존재하므로 Fargate의 서버리스 장점 우선
    
3. 배포 자동화 및 안정성
- Task 단위 Auto Scaling으로 트래픽 변화에 즉각 대응 용이
- CI/CD 파이프라인과의 자연스러운 통합 
   - Rolling Update 기본 제공, 필요 시 Blue/Green, Canary 배포로 전환 용이
- ECR과 연계해 배포 속도 증가

<br/>

### 4️⃣ CI/CD 파이프라인 최적화
**문제 상황**
초기 GitHub Actions 빌드 시 매번 전체 의존성 다운로드로 빌드 시간 길어지고 배포 속도 저하로 빠른 개발 사이클 구축 어려움

**해결 과정**
- Phase 1: Multi-stage Dockerfile 최적화
   - Gradle 의존성 캐시 레이어 분리 및 JRE 경량화
   - 이미지 크기 약 40% 이상 감소

- Phase 2: CI/CD 워크플로우 분리 및 캐싱 전략
   - CI 단계: GitHub Actions `actions/cache`로 Gradle 의존성 캐싱
   - CD 단계: ECR 푸시 및 배포 분리 (Blue/Green 무중단 배포 포함)
   - Dockerfile과 워크플로우 역할 분리로 캐싱 중복 제거

- Phase 3: Private Subnet 배포를 위한 Self-hosted Runner
   - 문제: GitHub-hosted Runner는 WAS Private Subnet 내 접근 불가 
   - 해결: Private Subnet EC2에 Self-hosted Runner 구축
      - VPC 내부에서 CD 작업 수행
      - ECR Layer Caching 효과 극대화 (동일 VPC 내부 통신)

- Phase 4: ECS Fargate 통합으로 파이프라인 단순화
   - Self-hosted Runner 제거 (ECS가 Private Subnet에서 테스크 직접 실행)
   - ECS Service Rolling Update로 무중단 배포 자동화

**성과**
- 빌드 시간 약 30% 단축 (의존성 재사용 효과)
- 배포 자동화로 수동 작업 대폭 감소
- 무중단 배포 및 Health Check 기반 자동 롤백 구현

**확장 가능성**
- ECS Service 특성으로 Blue/Green, Canary 배포 전환 용이
- 추가 마이크로서비스 확장 시에도 동일 파이프라인 재사용 가능

---

## 프로젝트 회고
이 프로젝트를 통해 "좋은 기술"이 아닌 "적합한 기술"을 선택하는 기준을 세울 수 있었습니다. Redis, Kubernetes 같은 검증된 기술들도 고려했지만, 현재 규모에서는 In-Memory 캐싱과 ECS Fargate가 더 적합하다고 판단했습니다.     
다만, 실제로 ECS Task를 다중으로 운영하며 분산 환경에서 인메모리 캐싱의 한계를 직접 경험했습니다. 각 Task가 독립적인 캐시를 가지면서 조회수 동기화에 오차가 발생했고, 데이터 정합성이 중요한 데이터는 아니지만, 이를 통해 초기 단계에서도 상태 공유가 필요한 데이터는 Redis 같은 중앙 집중식 캐시가 필요하다는 것을 깨달았습니다. 기술 선택은 현재에 적합해야 하지만, 실제 운영 환경에서 발생하는 문제를 통해 다음 단계의 필요성을 미리 파악할 수 있었습니다.
     
모든 선택에는 Trade-off가 있었습니다. ECS Fargate는 비용이 높지만 운영 부담을 줄이고, JWT는 다양한 위험에 대응해야 하고 비교적 복잡하지만 확장성이 뛰어납니다. 단순히 성능이나 비용 하나만 보는 것이 아니라, 현재 요구사항과 미래 확장성의 균형을 고려하는 의사결정 능력을 기를 수 있었습니다.
     
또한 점진적 개선의 가치를 체감했습니다. EC2 → ECS Fargate로 이어지는 인프라 진화뿐만 아니라, 기본 구조 → 성능 최적화 → 비용 효율화 → 모니터링까지 단계적으로 확장하며, 처음부터 이론적 완벽함을 추구하기보다 작은 단계부터라도 일단 배포를 하고 각 단계에서 문제점을 찾고 개선하는 것이 더 견고한 시스템을 만든다는 것을 배웠습니다.
     
