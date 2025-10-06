package com.samsamotot.otboo.common.security.jwt;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 무효화(블랙리스트 및 사용자 단위 컷오프) 관리를 위한 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenInvalidationService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:"; // + {jti}
    private static final String USER_INVALID_AFTER_PREFIX = "jwt:user:"; // + {userId}:invalidAfter

    /**
     * 주어진 jti(JWT ID)를 블랙리스트에 등록합니다.
     * 
     * @param jti        블랙리스트에 등록할 JWT ID
     * @param ttlSeconds 블랙리스트 유지 시간(초)
     */
    public void blacklistJti(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank()) return;
        if (ttlSeconds <= 0) {
            // TTL이 0 이하이면 등록할 필요가 없으므로 조기 종료
            return;
        }
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, Boolean.TRUE, ttlSeconds, TimeUnit.SECONDS);
        log.debug("블랙리스트 등록 - jti: {}, ttl: {}s", jti, ttlSeconds);
    }

    /**
     * 주어진 jti(JWT ID)가 블랙리스트에 등록되어 있는지 확인합니다.
     * 
     * @param jti 확인할 JWT ID
     * @return 블랙리스트에 있으면 true, 아니면 false
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        String key = BLACKLIST_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 특정 사용자의 invalidAfter(컷오프) 시각을 설정합니다.
     * 
     * @param userId       컷오프를 설정할 사용자 ID
     * @param invalidAfter 컷오프 시각(Instant)
     */
    public void setUserInvalidAfter(String userId, Instant invalidAfter) {
        if (userId == null || userId.isBlank() || invalidAfter == null) return;
        String key = buildInvalidAfterKey(userId);
        // 값은 epoch millis로 저장
        redisTemplate.opsForValue().set(key, invalidAfter.toEpochMilli());
        log.info("사용자 컷오프 설정 - userId: {}, invalidAfter: {}", userId, invalidAfter);
    }

    /**
     * 특정 사용자의 invalidAfter(컷오프) 시각을 epoch millis로 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 컷오프 시각(epoch millis), 없으면 null
     */
    public Long getUserInvalidAfterMillis(String userId) {
        if (userId == null || userId.isBlank()) return null;
        Object value = redisTemplate.opsForValue().get(buildInvalidAfterKey(userId));
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value != null ? Long.parseLong(String.valueOf(value)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 사용자 ID로부터 컷오프 Redis 키를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return 컷오프 Redis 키
     */
    private String buildInvalidAfterKey(String userId) {
        return USER_INVALID_AFTER_PREFIX + userId + ":invalidAfter";
    }
}


