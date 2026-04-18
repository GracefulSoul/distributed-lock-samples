# Distributed Lock Samples

Java 21 + Spring Boot 3.4.0 기반의 데이터 동시성 제어 샘플 프로젝트

## 개요

이 프로젝트는 3가지 데이터 동시성 제어 방식을 구현합니다:

1. **낙관락(Optimistic Locking)**: `@Version` 필드 기반 충돌 감지 및 재시도 메커니즘
2. **비관락(Pessimistic Locking)**: `SELECT FOR UPDATE`를 통한 행 수준 잠금
3. **분산락(Distributed Lock)**: Redis 기반 다중 인스턴스 환경 제어 (Redisson)

## 기술 스택

- **Java**: 21 (LTS, Spring Boot 3.4.0 공식 지원)
- **Spring Boot**: 3.4.0
- **Spring Data JPA**: 3.4.0
- **Hibernate**: 6.6.2.Final
- **Database**: H2 2.3.232 (In-Memory)
- **Cache**: Redis (선택사항 - Redis 프로필)
- **Distributed Lock**: Redisson 3.24.3 (선택사항 - Redis 프로필)
- **Build Tool**: Maven 3.9.14+
- **Lombok**: 1.18.44 (Annotation Processor 활성화)

## 빌드 및 실행

### 1. Maven을 이용한 빌드

```bash
# 전체 빌드 (JAR 생성)
mvn clean package

# 테스트 스킵하고 빌드
mvn clean package -DskipTests
```

### 2. Maven을 이용한 직접 실행

```bash
# In-Memory 프로필로 직접 실행 (기본값)
mvn spring-boot:run

# Redis 프로필로 직접 실행
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=redis"
```

### 3. JAR 파일로 실행

```bash
# In-Memory 프로필로 실행
java -jar target/distributed-lock-samples-1.0.0.jar --spring.profiles.active=in-memory

# Redis 프로필로 실행
java -jar target/distributed-lock-samples-1.0.0.jar --spring.profiles.active=redis
```

**애플리케이션 시작 확인**:
```
Started DistributedLockSamplesApplication in X.XXX seconds
Tomcat started on port 8080 (http) with context path '/'
========== 샘플 데이터 초기화 완료 ==========
```

---

## 프로필 설정

### 1. In-Memory 프로필 (기본값)

낙관락과 비관락만 사용하는 기본 프로필입니다. Redis는 필요 없습니다.

```bash
# Maven 실행
mvn spring-boot:run

# 또는 프로필명시
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=in-memory"

# 또는 JAR 파일 실행
java -jar target/distributed-lock-samples-1.0.0.jar --spring.profiles.active=in-memory
```

**설정 파일**: `application-in-memory.properties`

**특징**:
- ✅ H2 In-Memory 데이터베이스 (자동 생성/초기화)
- ✅ 낙관락(Optimistic Lock) - `@Version` 기반
- ✅ 비관락(Pessimistic Lock) - `SELECT FOR UPDATE`
- ❌ 분산락 제외 (Redis 자동설정 제외됨)
- ⚡ 빠른 시작, Redis 설치 불필요

**자동설정 제외**:
```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,\
  org.redisson.spring.starter.RedissonAutoConfigurationV2
```

---

### 2. Redis 프로필

Redis를 활용한 분산락을 포함한 모든 기능을 사용합니다.

```bash
# Redis 프로필 활성화
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=redis"

# 또는 JAR 파일 실행
java -jar target/distributed-lock-samples-1.0.0.jar --spring.profiles.active=redis

# 커스텀 Redis 호스트 지정
java -jar target/distributed-lock-samples-1.0.0.jar \
  --spring.profiles.active=redis \
  --spring.redis.host=your.redis.host \
  --spring.redis.port=6380
```

**설정 파일**: `application-redis.properties`

**특징**:
- ✅ 낙관락(Optimistic Lock)
- ✅ 비관락(Pessimistic Lock)
- ✅ 분산락(Distributed Lock with Redisson)
- 📊 다중 인스턴스 환경 지원
- 🔒 Redisson 기반 안전한 분산 잠금

#### Redis 설치 및 실행 (Windows)

**Option 1: WSL2에서 Redis 실행** (권장)
```bash
# WSL2 터미널에서
wsl -d Ubuntu
redis-server
```

**Option 2: Docker에서 Redis 실행**
```bash
docker run -d -p 6379:6379 redis:latest
docker logs -f <container-id>  # 로그 확인
```

**Option 3: Windows Redis 바이너리**
- [Microsoft/redis](https://github.com/microsoftarchive/redis/releases){:target="_blank"} 다운로드
- 또는 [Memurai](https://www.memurai.com/){:target="_blank"} 설치

#### Redis 연결 확인
```bash
redis-cli
> PING
PONG

# Redis 정보 확인
> INFO server
# redis_version, redis_mode 등 확인 가능
```

---

## 프로젝트 구조

```
distributed-lock-samples/
├── pom.xml                                    # Maven 설정 (Java 25, Spring Boot 3.4.0)
├── README.md                                  # 이 문서
│
├── src/main/
│   ├── java/com/gracefulsoul/lock/
│   │   ├── DistributedLockSamplesApplication.java   # 메인 애플리케이션
│   │   ├── DataInitializer.java                     # 샘플 데이터 초기화
│   │   │
│   │   ├── config/
│   │   │   └── RedissonConfig.java                  # Redis 설정 (@Profile("redis"))
│   │   │
│   │   ├── domain/
│   │   │   ├── OptimisticAccount.java               # 낙관락 엔티티
│   │   │   ├── PessimisticAccount.java              # 비관락 엔티티
│   │   │   └── DistributedAccount.java              # 분산락 엔티티
│   │   │
│   │   ├── repository/
│   │   │   ├── OptimisticAccountRepository.java     # 낙관락 저장소
│   │   │   ├── PessimisticAccountRepository.java    # 비관락 저장소
│   │   │   └── DistributedAccountRepository.java    # 분산락 저장소
│   │   │
│   │   ├── service/
│   │   │   ├── OptimisticLockService.java           # 낙관락 서비스
│   │   │   ├── PessimisticLockService.java          # 비관락 서비스
│   │   │   └── DistributedLockService.java          # 분산락 서비스 (@Profile("redis"))
│   │   │
│   │   └── controller/
│   │       └── AccountController.java               # REST API 컨트롤러
│   │
│   └── resources/
│       ├── application.properties                   # 기본 설정
│       ├── application-in-memory.properties         # In-Memory 프로필 설정
│       └── application-redis.properties             # Redis 프로필 설정
│
└── src/test/
    └── java/com/gracefulsoul/lock/service/
        ├── OptimisticLockServiceTest.java           # 낙관락 테스트
        └── PessimisticLockServiceTest.java          # 비관락 테스트
```

---

## API 사용 예제

> **API 테스트**: 애플리케이션이 실행 중일 때 `http://localhost:8080`에서 아래 명령어를 실행하세요.

### 낙관락 API (In-Memory / Redis)

낙관락은 **충돌이 자주 발생하지 않는 환경**에 적합합니다. 데이터 버전을 비교하여 충돌을 감지합니다.

```bash
# 낙관락 이체 (OPT-001 → OPT-002로 1000원 이체)
curl -X POST "http://localhost:8080/api/accounts/optimistic/transfer?from=OPT-001&to=OPT-002&amount=1000"
# 응답: 낙관락 이체 성공

# 낙관락 입금
curl -X POST "http://localhost:8080/api/accounts/optimistic/deposit?accountNumber=OPT-001&amount=5000"
# 응답: 낙관락 입금 성공

# 낙관락 출금
curl -X POST "http://localhost:8080/api/accounts/optimistic/withdraw?accountNumber=OPT-001&amount=2000"
# 응답: 낙관락 출금 성공
```

### 비관락 API (In-Memory / Redis)

비관락은 **충돌이 빈번한 환경**에 적합합니다. 행을 미리 잠금으로써 동시성 문제를 원천 차단합니다.

```bash
# 비관락 이체 (PES-001 → PES-002로 1000원 이체)
curl -X POST "http://localhost:8080/api/accounts/pessimistic/transfer?from=PES-001&to=PES-002&amount=1000"
# 응답: 비관락 이체 성공

# 비관락 입금
curl -X POST "http://localhost:8080/api/accounts/pessimistic/deposit?accountNumber=PES-001&amount=5000"
# 응답: 비관락 입금 성공

# 비관락 출금
curl -X POST "http://localhost:8080/api/accounts/pessimistic/withdraw?accountNumber=PES-001&amount=2000"
# 응답: 비관락 출금 성공

# 비관락 잔액 조회
curl "http://localhost:8080/api/accounts/pessimistic/balance?accountNumber=PES-001"
# 응답: {"accountNumber":"PES-001","balance":XXXX}
```

### 분산락 API (Redis 프로필만)

분산락은 **마이크로서비스 또는 클러스터 환경**에서 여러 인스턴스 간 동시성을 제어합니다.

```bash
# 분산락 이체 (DIS-001 → DIS-002로 1000원 이체)
curl -X POST "http://localhost:8080/api/accounts/distributed/transfer?from=DIS-001&to=DIS-002&amount=1000"
# 응답: 분산락 이체 성공 (Redis 프로필에서만 가능)

# 분산락 입금
curl -X POST "http://localhost:8080/api/accounts/distributed/deposit?accountNumber=DIS-001&amount=5000"
# 응답: 분산락 입금 성공

# 분산락 출금
curl -X POST "http://localhost:8080/api/accounts/distributed/withdraw?accountNumber=DIS-001&amount=2000"
# 응답: 분산락 출금 성공
```

**Redis 프로필 미활성화 시**:
```json
{
  "error": "분산락 기능을 사용하려면 Redis 프로필을 활성화하세요: --spring.profiles.active=redis"
}
```

---

## 테스트 실행

```bash
# 전체 테스트 실행
mvn test

# 특정 테스트 클래스만 실행
mvn test -Dtest=OptimisticLockServiceTest
mvn test -Dtest=PessimisticLockServiceTest

# 테스트 스킵하고 빌드
mvn clean install -DskipTests
```

---

## 구현 상세사항

### 1. 낙관락(Optimistic Locking)

**원리**: 버전 필드(`@Version`)를 사용하여 업데이트 시 버전 충돌을 감지합니다.

**코드**:
```java
@Entity
@Table(name = "optimistic_account")
@Data
public class OptimisticAccount {
    @Id
    private String accountNumber;
    
    private Long balance;
    
    @Version  // 낙관락 버전 필드
    private Long version;
}
```

**장점**:
- ✅ 읽기 성능 우수 (잠금 없음)
- ✅ 데드락 없음
- ✅ 가벼운 구현

**단점**:
- ❌ 충돌 시 재시도 필요
- ❌ 높은 경합 환경에서 성능 저하

**사용 시나리오**:
- 블로그 댓글, 좋아요 시스템
- 재고 업데이트 (충돌 빈도 낮음)
- 사용자 프로필 수정

---

### 2. 비관락(Pessimistic Locking)

**원리**: `SELECT FOR UPDATE` 쿼리로 데이터를 미리 잠금합니다.

**코드**:
```java
@Repository
public interface PessimisticAccountRepository extends JpaRepository<PessimisticAccount, String> {
    @Query("SELECT a FROM PessimisticAccount a WHERE a.accountNumber = :accountNumber")
    @Lock(LockModeType.PESSIMISTIC_WRITE)  // 배타적 잠금
    PessimisticAccount findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
```

**장점**:
- ✅ 높은 경합 환경에서 성능 우수
- ✅ 데드락 가능성 낮음
- ✅ 구현 단순함

**단점**:
- ❌ 읽기 성능 저하
- ❌ 데드락 가능성 존재
- ❌ 동시성 감소

**사용 시나리오**:
- 좌석 예약 시스템 (높은 경합)
- 재고 감소 작업
- 금융 거래 (중요 업무)

---

### 3. 분산락(Distributed Lock)

**원리**: Redis의 `SET NX EX` 명령어 또는 Redisson 라이브러리를 사용합니다.

**코드**:
```java
@Service
@Profile("redis")  // Redis 프로필에서만 활성화
public class DistributedLockService {
    private final RedissonClient redissonClient;
    
    public void transfer(String from, String to, Long amount) {
        RLock lock1 = redissonClient.getLock(from);
        RLock lock2 = redissonClient.getLock(to);
        RLock multiLock = redissonClient.getMultiLock(lock1, lock2);
        
        if (multiLock.tryLock(3, 10, TimeUnit.SECONDS)) {
            try {
                // 이체 로직
            } finally {
                multiLock.unlock();
            }
        }
    }
}
```

**특징**:
- ✅ 다중 인스턴스 환경 지원
- ✅ Watch Dog 메커니즘 (자동 연장)
- ✅ 데드락 방지 (정렬된 잠금 획득)
- ✅ 마이크로서비스 환경 최적화

**사용 시나리오**:
- 마이크로서비스 환경
- Kubernetes 클러스터 환경
- 분산 트랜잭션 조율
- 글로벌 리소스 제어

---

## 환경 설정 정보

| 항목 | 버전 |
|------|------|
| Java | 21 (LTS) |
| Spring Boot | 3.4.0 |
| Spring Framework | 6.2.0 |
| Hibernate | 6.6.2.Final |
| H2 Database | 2.3.232 |
| Maven | 3.9.14+ |
| Redisson | 3.24.3 (Redis 프로필) |
| Lombok | 1.18.44 |

---

## 문제 해결 가이드

### Q1: In-Memory 프로필에서 Redis 연결 오류 발생
**원인**: Redis 자동설정이 활성화된 경우
**해결**: `application-in-memory.properties`에서 Redis 자동설정 제외 확인
```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,\
  org.redisson.spring.starter.RedissonAutoConfigurationV2
```

### Q2: 분산락 API에서 503 에러 발생
**원인**: Redis 프로필이 활성화되지 않음
**해결**: 애플리케이션 시작 시 `--spring.profiles.active=redis` 옵션 추가

### Q3: Redis 연결 안 됨 (Connection refused)
**원인**: Redis 서버가 실행 중이지 않음
**해결**: 
```bash
# Redis 상태 확인
redis-cli ping

# WSL2에서 Redis 실행
wsl -d Ubuntu && redis-server
```

### Q4: 낙관락 충돌 발생 (`OptimisticLockingFailureException`)
**원인**: 동시에 같은 데이터를 수정하려는 시도
**해결**: OptimisticLockService의 재시도 로직 확인 (최대 3회)

---

## 참고 자료

- [Spring Data JPA - @Lock Documentation](https://docs.spring.io/spring-data/jpa/reference/jpa.html){:target="_blank"}
- [Redisson - Documentation](https://redisson.org/){:target="_blank"}
- [H2 Database - Console](http://localhost:8080/h2-console){:target="_blank"} (In-Memory 프로필 실행 시)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles){:target="_blank"}

---

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.


## 빌드

```bash
# 프로젝트 빌드
mvn clean install

# JAR 파일 생성
mvn clean package

# 실행 가능한 JAR
java -jar target/distributed-lock-samples-1.0.0.jar
```

### 프로필과 함께 실행

```bash
# In-Memory 모드로 실행
java -jar target/distributed-lock-samples-1.0.0.jar --spring.profiles.active=in-memory

# Redis 모드로 실행
java -jar target/distributed-lock-samples-1.0.0.jar --spring.profiles.active=redis
```

---

## 프로필 조건부 로드 메커니즘

### 1. In-Memory 프로필 (@Profile("in-memory"))

- **자동 로드**: `application-in-memory.properties` 활성화
- **Bean 생성**: 기본 서비스만 로드
- **Redis 의존성**: 필요 없음
- **사용 사례**: 개발, 테스트, 단일 인스턴스 환경

### 2. Redis 프로필 (@Profile("redis"))

- **자동 로드**: `application-redis.properties` 활성화
- **Bean 생성**:
  - `RedissonConfig` - Redis 연결
  - `DistributedLockService` - 분산락 서비스
- **조건부 활성화**: `RedissonConfig`에서 `@ConditionalOnProperty("spring.redis.host")`
- **사용 사례**: 프로덕션, 마이크로서비스, 다중 인스턴스 환경

---

## 핵심 설정 값

### In-Memory (기본)

```properties
# 기본 프로필
spring.profiles.active=in-memory

# 데이터베이스
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

### Redis

```properties
# Redis 프로필
spring.profiles.active=redis

# Redis 연결
spring.redis.host=localhost
spring.redis.port=6379

# 연결 풀
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
```

---

## 의존성

### 필수 의존성

- Spring Boot 3.4.0
- Spring Data JPA
- H2 Database
- Lombok

### 선택적 의존성 (Redis 프로필)

- Spring Data Redis
- Redisson 3.24.3

```xml
<!-- pom.xml에서 optional 태그로 설정 -->
<optional>true</optional>
```

---

## 문제 해결

### Redis 연결 실패

**증상**: `Cannot connect to Redis` 에러

**해결방법**:
```bash
# 1. Redis 서버 실행 확인
redis-cli PING
# 응답: PONG

# 2. 호스트/포트 확인
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.redis.host=localhost --spring.redis.port=6379"

# 3. In-Memory 모드로 전환
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=in-memory"
```

### Redis 없이 분산락 API 호출

**증상**: 404 또는 서비스 불가능

**해결방법**:
```bash
# 1. Redis 프로필 활성화 확인
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=redis"

# 2. 실행 중인 프로필 확인 (로그에서)
# "The following profiles are active: redis"

# 3. API 엔드포인트 확인
curl "http://localhost:8080/api/accounts/distributed/transfer?from=DIS-001&to=DIS-002&amount=1000"
```

---

## 참고 자료

### 공식 문서

- [Spring Boot 3.4.0 Documentation](https://spring.io/projects/spring-boot){:target="_blank"}
- [Spring Data JPA - Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods/query-method-details.html#jpa.locking){:target="_blank"}
- [Redisson Documentation](https://redisson.org/){:target="_blank"}

### 블로그 포스트

- [GracefulSoul - 데이터 동시성 제어](https://gracefulsoul.github.io/database/data-concurrency-lock/){:target="_blank"}

---

## 라이선스

MIT License - 자유롭게 사용, 수정, 배포 가능합니다.

## 작성자

GracefulSoul (Kim)  
Seoul, Republic of Korea

---

**마지막 업데이트**: 2026-04-18  
**Java 버전**: 25  
**Spring Boot 버전**: 3.4.0
