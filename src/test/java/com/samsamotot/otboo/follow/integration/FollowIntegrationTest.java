package com.samsamotot.otboo.follow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowListResponse;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

/**
 * PackageName  : com.samsamotot.otboo.follow.integration
 * FileName     : FollowIntegrationTest
 * Author       : dounguk
 * Date         : 2025. 9. 14.
 * Description  : follow 기능을 통합 테스트 하는 테스트 클래스
 */

@DisplayName("Follow 통합 테스트")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = AFTER_CLASS)
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

    @Test
    void 팔로잉_목록_조회를_한다() throws Exception {
        User follower = User.createUser("f1@test.com", "follower", "password#A", BCRYPT_PASSWORD_ENCODER);
        userRepository.save(follower);

        User followee1 = User.createUser("e1@test.com", "followee1", "password#A", BCRYPT_PASSWORD_ENCODER);
        User followee2 = User.createUser("e2@test.com", "followee2", "password#A", BCRYPT_PASSWORD_ENCODER);
        User followee3 = User.createUser("e3@test.com", "followee3", "password#A", BCRYPT_PASSWORD_ENCODER);
        userRepository.saveAll(java.util.List.of(followee1, followee2, followee3));

        followService.follow(new FollowCreateRequest(follower.getId(), followee1.getId()));
        Thread.sleep(5);
        followService.follow(new FollowCreateRequest(follower.getId(), followee2.getId()));
        Thread.sleep(5);
        followService.follow(new FollowCreateRequest(follower.getId(), followee3.getId()));

        FollowingRequest page1Req = new FollowingRequest(follower.getId(), null, null, 2, null);
        FollowListResponse page1 = followService.getFollowings(page1Req);

        assertThat(page1).isNotNull();
        assertThat(page1.data()).isNotNull();
        assertThat(page1.data().size()).isLessThanOrEqualTo(2);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.nextCursor()).isNotNull();
        assertThat(page1.nextIdAfter()).isNotNull();

        page1.data().forEach(d ->
            assertThat(d.follower().userId()).isEqualTo(follower.getId())
        );

        FollowingRequest page2Req = new FollowingRequest(
            follower.getId(),
            page1.nextCursor(),          // createdAt 커서
            page1.nextIdAfter(),         // id 보조커서
            2,
            null
        );
        FollowListResponse page2 = followService.getFollowings(page2Req);

        assertThat(page2).isNotNull();
        assertThat(page2.data()).isNotNull();
        assertThat(page2.hasNext()).isFalse();
        assertThat(page2.nextCursor()).isNull();
        assertThat(page2.nextIdAfter()).isNull();

        java.util.Set<UUID> p1Followees = page1.data().stream()
            .map(d -> d.followee().userId())
            .collect(java.util.stream.Collectors.toSet());
        assertThat(page2.data().stream().map(d -> d.followee().userId()))
            .doesNotContainAnyElementsOf(p1Followees);
    }
}
