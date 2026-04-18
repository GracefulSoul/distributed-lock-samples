# Distributed Lock Samples

Java 25 + Spring Boot 3.4.0 기반의 데이터 동시성 제어 샘플 프로젝트

## 개요

이 프로젝트는 3가지 데이터 동시성 제어 방식을 구현합니다:

1. **낙관락(Optimistic Locking)**: 버전 필드 기반 충돌 감지
2. **비관락(Pessimistic Locking)**: SELECT FOR UPDATE를 통한 행 잠금
3. **분산락(Distributed Lock)**: Redis 기반 다중 인스턴스 환경 제어

## 기술 스택

- **Java**: 25
- **Spring Boot**: 3.4.0
- **Database**: H2 (In-Memory)
- **Cache**: Redis (Optional)
- **ORM**: Spring Data JPA + Hibernate
- **Distributed Lock**: Redisson (Optional)
- **Build Tool**: Maven

## 프로필 설정

### 1. In-Memory 프로필 (기본값)

낙관락과 비관락만 사용하는 기본 프로필입니다. Redis는 필요 없습니다.

```bash
# Maven 실행
mvn spring-boot:run

# 또는 프로필명시
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=in-memory"
```

**설정 파일**: `application-in-memory.properties`

**포함 내용**:
- ✅ 낙관락(Optimistic Lock)
- ✅ 비관락(Pessimistic Lock)
- ❌ 분산락(Redis 미사용)

---

### 2. Redis 프로필

Redis를 활용한 분산락을 포함한 모든 기능을 사용합니다.

#### Redis 설치 (Windows)

**Option 1: WSL2에서 Redis 실행**
```bash
# WSL2 터미널에서
wsl -d Ubuntu
redis-server
```

**Option 2: Docker에서 Redis 실행**
```bash
docker run -d -p 6379:6379 redis:latest
```

**Option 3: Windows Redis 바이너리**
- [Microsoft/redis](https://github.com/microsoftarchive/redis/releases){:target="_blank"} 다운로드
- 또는 [Memurai](https://www.memurai.com/){:target="_blank"} 설치

#### Redis 연결 확인
```bash
redis-cli
> PING
PONG
```

#### Maven으로 Redis 프로필 실행

```bash
# 기본 설정 (localhost:6379)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=redis"

# 커스텀 Redis 호스트 지정
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=redis --spring.redis.host=your.redis.host --spring.redis.port=6380"
```

**설정 파일**: `application-redis.properties`

**포함 내용**:
- ✅ 낙관락(Optimistic Lock)
- ✅ 비관락(Pessimistic Lock)
- ✅ 분산락(Distributed Lock with Redis)

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

### 낙관락 API

```bash
# 낙관락 이체
curl -X POST "http://localhost:8080/api/accounts/optimistic/transfer?from=OPT-001&to=OPT-002&amount=1000"

# 낙관락 입금
curl -X POST "http://localhost:8080/api/accounts/optimistic/deposit?accountNumber=OPT-001&amount=5000"

# 낙관락 출금
curl -X POST "http://localhost:8080/api/accounts/optimistic/withdraw?accountNumber=OPT-001&amount=2000"
```

### 비관락 API

```bash
# 비관락 이체
curl -X POST "http://localhost:8080/api/accounts/pessimistic/transfer?from=PES-001&to=PES-002&amount=1000"

# 비관락 입금
curl -X POST "http://localhost:8080/api/accounts/pessimistic/deposit?accountNumber=PES-001&amount=5000"

# 비관락 출금
curl -X POST "http://localhost:8080/api/accounts/pessimistic/withdraw?accountNumber=PES-001&amount=2000"

# 비관락 잔액 조회
curl "http://localhost:8080/api/accounts/pessimistic/balance?accountNumber=PES-001"
```

### 분산락 API (Redis 프로필)

```bash
# 분산락 이체
curl -X POST "http://localhost:8080/api/accounts/distributed/transfer?from=DIS-001&to=DIS-002&amount=1000"

# 분산락 입금
curl -X POST "http://localhost:8080/api/accounts/distributed/deposit?accountNumber=DIS-001&amount=5000"

# 분산락 출금
curl -X POST "http://localhost:8080/api/accounts/distributed/withdraw?accountNumber=DIS-001&amount=2000"
```

---

## 테스트 실행

```bash
# 전체 테스트
mvn test

# 특정 테스트 클래스만 실행
mvn test -Dtest=OptimisticLockServiceTest

# 테스트 스킵
mvn clean install -DskipTests
```

---

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
