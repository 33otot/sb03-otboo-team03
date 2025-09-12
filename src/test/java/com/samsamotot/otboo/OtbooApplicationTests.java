package com.samsamotot.otboo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // 테스트 시 test 프로파일 명시적 활성화
class OtbooApplicationTests {

	@Test
	void contextLoads() {
	}

}
