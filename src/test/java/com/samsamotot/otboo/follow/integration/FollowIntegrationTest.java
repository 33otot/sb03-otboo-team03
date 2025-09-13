package com.samsamotot.otboo.follow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.follow.service.FollowService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PackageName  : com.samsamotot.otboo.follow.integration
 * FileName     : FollowIntegrationTest
 * Author       : dounguk
 * Date         : 2025. 9. 14.
 */

@DisplayName("Follow 통합 테스트")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
//    "spring.sql.init.mode=never",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
public class FollowIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url",      postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private FollowService followService;

    @Autowired
    ObjectMapper objectMapper;

    BCryptPasswordEncoder BCRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Test
    void 팔로우를_정상적으로_생성한다() throws Exception {
        // given
        User follower = User.createUser("test@test.com", "tester1", "password#A", BCRYPT_PASSWORD_ENCODER);
        userRepository.save(follower);

        User followee = User.createUser("test2@test.com", "tester2", "password#A", BCRYPT_PASSWORD_ENCODER);
        userRepository.save(followee);

        FollowCreateRequest request = new FollowCreateRequest(follower.getId(), followee.getId());

        // when
        FollowDto response = followService.follow(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.follower()).isNotNull();
        assertThat(response.follower().userId()).isEqualTo(follower.getId());
        assertThat(response.followee()).isNotNull();
        assertThat(response.followee().userId()).isEqualTo(followee.getId());
    }
}
