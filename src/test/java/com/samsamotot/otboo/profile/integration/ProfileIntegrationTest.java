package com.samsamotot.otboo.profile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.fixture.ProfileFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional // ✨ 1. 각 테스트 후 DB 롤백을 위해 @Transactional 추가
@ActiveProfiles("test") // ✨ 2. 테스트용 application.yaml을 사용하도록 명시
@DisplayName("프로필 API 통합 테스트")
public class ProfileIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PasswordEncoder passwordEncoder; // 비밀번호 암호화를 위해 필요


    @BeforeEach
    void setUp() {
        // ✨ 3. 테스트 독립성을 위해 매번 데이터를 초기화
        userRepository.deleteAll();

        User user = UserFixture.createValidUser(); // "test@example.com" 사용자를 생성
        user.encodePassword(passwordEncoder); // ✨ 4. 비밀번호를 암호화해야 Security가 인증 가능
        userRepository.save(user);

        Profile profile = Profile.builder().user(user).build();
        profileRepository.save(profile);
    }

    @Test
    @WithUserDetails("test@example.com")
    void 날씨_알림_수신_여부를_true로_업데이트_성공() throws Exception {
        mockMvc.perform(patch("/api/users/profiles/notification-weathers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weatherNotificationEnabled\":true}"))
                .andExpect(status().isOk());

        UUID userId = userRepository.findByEmail("test@example.com")
                .orElseThrow(() -> new RuntimeException("테스트용 유저가 존재하지 않음"))
                .getId();

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow();

        assertTrue(profile.isWeatherNotificationEnabled());
    }
}
