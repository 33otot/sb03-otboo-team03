package com.samsamotot.otboo.common.security.jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
public class TokenInvalidationServiceTest {

  @Test
  @DisplayName("ttl<=0이면 블랙리스트 등록을 건너뛴다")
  void blacklist_skip_on_zero_ttl() {
    RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    TokenInvalidationService svc = new TokenInvalidationService(redis);
    // 동작만 호출, 예외 없이 통과
    svc.blacklistJti("jti", 0);
  }

  @Test
  @DisplayName("invalidAfter 저장 후 조회가 가능하다")
  void set_and_get_invalidAfter() {
    RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    ValueOperations<String, Object> ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);

    TokenInvalidationService svc = new TokenInvalidationService(redis);
    Instant now = Instant.now();
    svc.setUserInvalidAfter("user-1", now);

    // get 호출 시 mock 동작 정의
    when(ops.get("jwt:user:user-1:invalidAfter")).thenReturn(now.toEpochMilli());
    Long read = svc.getUserInvalidAfterMillis("user-1");
    assertThat(read).isEqualTo(now.toEpochMilli());
  }
}


