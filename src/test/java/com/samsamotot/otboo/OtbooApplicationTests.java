package com.samsamotot.otboo;

import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // 테스트 시 test 프로파일 명시적 활성화
@Import({TestConfig.class, SecurityTestConfig.class})  // 테스트용 PasswordEncoder 빈 포함
class OtbooApplicationTests {

	@Test
	void contextLoads() {
	}

}
