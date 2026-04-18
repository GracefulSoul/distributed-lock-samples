package com.gracefulsoul.lock.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 분산락(Distributed Lock) - Redis 설정 클래스
 *
 * 활성화 조건:
 * 1. spring.profiles.active=redis
 * 또는
 * 2. spring.profiles.active에 redis가 포함된 경우
 *
 * 사용 방법:
 * - 개발: mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=redis"
 * - 배포: 환경변수 SPRING_PROFILES_ACTIVE=redis 설정
 */
@Configuration
@Profile("redis")
@ConditionalOnProperty(name = "spring.redis.host", havingValue = "localhost", matchIfMissing = false)
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * Redis 기반 분산락 설정
     * - 단일 Redis 서버 연결
     * - 연결 풀 설정 (최대 64개 동시 연결)
     * - 자동 재연결 설정
     *
     * @return RedissonClient
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        String address = "redis://" + redisHost + ":" + redisPort;
        
        config.useSingleServer()
            .setAddress(address)
            .setPassword(redisPassword.isEmpty() ? null : redisPassword)
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10)
            .setRetryAttempts(3)
            .setRetryInterval(1500)
            .setSubscriptionsPerConnection(5);

        return Redisson.create(config);
    }

}
