package com.samsamotot.otboo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import com.samsamotot.otboo.config.TestConfig;

@SpringBootTest
@ActiveProfiles("test")  // 테스트 시 test 프로파일 명시적 활성화
@Import(TestConfig.class)  // 테스트용 PasswordEncoder 빈 포함
class OtbooApplicationTests {

	@Test
	void contextLoads() {
	}

}
