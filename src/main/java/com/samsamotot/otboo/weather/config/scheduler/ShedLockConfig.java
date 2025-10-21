package com.samsamotot.otboo.weather.config.scheduler;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M") // 락 최대 유지 시간 10분
public class ShedLockConfig {

    @Value("${shedlock.env}")
    private String shedLockEnv;

    /**
     * ShedLock을 위한 Redis 기반 LockProvider Bean 등록
     * @param connectionFactory (기존에 설정된 RedisConnectionFactory)
     * @return RedisLockProvider
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        // 주입받은 환경 값("DEV" 또는 "PROD")을 네임스페이스(접두사)로 사용
        return new RedisLockProvider(connectionFactory, shedLockEnv);
    }
}
