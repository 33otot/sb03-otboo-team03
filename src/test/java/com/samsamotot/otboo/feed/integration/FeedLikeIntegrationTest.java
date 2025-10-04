package com.samsamotot.otboo.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedLike;
import com.samsamotot.otboo.feed.repository.FeedLikeRepository;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.GridRepository;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@Sql(statements = {
    "TRUNCATE TABLE feed_likes CASCADE",
    "TRUNCATE TABLE feeds CASCADE",
    "TRUNCATE TABLE clothes CASCADE",
    "TRUNCATE TABLE weathers CASCADE",
    "TRUNCATE TABLE grids CASCADE",
    "TRUNCATE TABLE users CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("FeedLike 통합 테스트")
public class FeedLikeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private GridRepository gridRepository;

    private User testUser;
    private User otherUser;
    private Feed testFeed;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(UserFixture.createUserWithEmail("testuser@example.com"));
        otherUser = userRepository.save(UserFixture.createUserWithEmail("otheruser@example.com"));
        Grid grid = gridRepository.save(GridFixture.createGrid());
        Weather weather = weatherRepository.save(WeatherFixture.createWeather(grid));
        testFeed = feedRepository.save(
            Feed.builder()
                .author(testUser)
                .weather(weather)
                .content("테스트 피드")
                .build()
        );
    }

    @Nested
    @DisplayName("피드 좋아요 생성/취소")
    class LikeFeed {

        @Test
        @WithUserDetails(
            value = "otheruser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 피드에_좋아요를_누르면_생성되고_likeCount가_증가한다() throws Exception {

            // when
            mockMvc.perform(post("/api/feeds/" + testFeed.getId() + "/like")
                    .with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(print());

            // then
            boolean exists = feedLikeRepository.existsByFeedIdAndUserId(testFeed.getId(), otherUser.getId());
            assertThat(exists).isTrue();

            Feed refreshed = feedRepository.findById(testFeed.getId()).orElseThrow();
            assertThat(refreshed.getLikeCount()).isEqualTo(1);
        }

        @Test
        @WithUserDetails(
            value = "otheruser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 이미_좋아요를_누른_피드에_중복_요청하면_409_에러를_반환한다() throws Exception {

            // given
            mockMvc.perform(post("/api/feeds/" + testFeed.getId() + "/like")
                    .with(csrf()))
                .andExpect(status().isNoContent());

            Feed afterFirst = feedRepository.findById(testFeed.getId()).orElseThrow();
            assertThat(afterFirst.getLikeCount()).isEqualTo(1L);

            // when & then
            mockMvc.perform(post("/api/feeds/" + testFeed.getId() + "/like")
                    .with(csrf()))
                .andExpect(status().isConflict())
                .andDo(print());

            Feed afterDuplicate = feedRepository.findById(testFeed.getId()).orElseThrow();
            assertThat(afterDuplicate.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @WithUserDetails(value = "otheruser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 존재하지_않는_피드에_좋아요를_누르면_404_에러를_반환한다() throws Exception {

            // given
            UUID missingId = UUID.randomUUID();

            // when & then
            mockMvc.perform(post("/api/feeds/" + missingId + "/like").with(csrf()))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithUserDetails(value = "otheruser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 삭제된_피드에는_좋아요를_생성할_수_없다() throws Exception {

            // given
            testFeed.delete();
            feedRepository.save(testFeed);

            // when & then
            mockMvc.perform(post("/api/feeds/" + testFeed.getId() + "/like").with(csrf()))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithUserDetails(
            value = "otheruser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 좋아요를_취소하면_삭제되고_likeCount가_감소한다() throws Exception {

            // given
            mockMvc.perform(post("/api/feeds/" + testFeed.getId() + "/like")
                    .with(csrf()))
                .andExpect(status().isNoContent());

            Feed afterFirst = feedRepository.findById(testFeed.getId()).orElseThrow();
            assertThat(afterFirst.getLikeCount()).isEqualTo(1L);

            // when
            mockMvc.perform(delete("/api/feeds/" + testFeed.getId() + "/like")
                    .with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(print());

            // then
            boolean exists = feedLikeRepository.existsByFeedIdAndUserId(testFeed.getId(), otherUser.getId());
            assertThat(exists).isFalse();

            Feed refreshed = feedRepository.findById(testFeed.getId()).orElseThrow();
            assertThat(refreshed.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @WithUserDetails(
            value = "otheruser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 없는_좋아요를_취소하면_404_에러를_반환한다() throws Exception {

            // when & then
            mockMvc.perform(delete("/api/feeds/" + testFeed.getId() + "/like")
                    .with(csrf()))
                .andExpect(status().isNotFound())
                .andDo(print());
        }
    }
}
