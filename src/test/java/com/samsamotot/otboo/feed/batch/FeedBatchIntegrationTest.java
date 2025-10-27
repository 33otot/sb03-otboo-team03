package com.samsamotot.otboo.feed.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.repository.CommentRepository;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.config.TestConfig;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBatchTest
@SpringBootTest(
    properties = {
        "spring.batch.job.enabled=false"   // 잡 자동 실행 OFF
    }
)
@ActiveProfiles("test")
@Import({TestConfig.class, SecurityTestConfig.class})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@Sql(statements = {
    "TRUNCATE TABLE feed_clothes CASCADE",
    "TRUNCATE TABLE feeds CASCADE",
    "TRUNCATE TABLE clothes CASCADE",
    "TRUNCATE TABLE weathers CASCADE",
    "TRUNCATE TABLE grids CASCADE",
    "TRUNCATE TABLE users CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("피드 배치 통합 테스트")
class FeedBatchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Job deleteSoftDeletedFeedsJob;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(deleteSoftDeletedFeedsJob);
    }

    @AfterEach
    void tearDown() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @TestConfiguration
    static class BatchTestSupportConfig {
        @Bean
        JobLauncherTestUtils jobLauncherTestUtils() {
            return new JobLauncherTestUtils();
        }
        @Bean
        JobRepositoryTestUtils jobRepositoryTestUtils(JobRepository jobRepository) {
            return new JobRepositoryTestUtils(jobRepository);
        }
    }

    @Test
    void 일주일_이상된_논리_삭제_피드와_관련_데이터를_영구_삭제한다() throws Exception {
        // given
        User user = userRepository.save(UserFixture.createUser());
        Grid grid = gridRepository.save(GridFixture.createGrid());
        Weather weather = weatherRepository.save(WeatherFixture.createWeather(grid));
        Instant eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS);
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);

        // 삭제 대상 피드 (8일 전 논리삭제)
        Feed feedToDelete = createFeed(user, weather, "삭제될 피드", true, eightDaysAgo);
        // 유지 대상 피드 1 (2일 전 논리삭제)
        Feed feedToKeep1 = createFeed(user, weather, "유지될 피드 1", true, twoDaysAgo);
        // 유지 대상 피드 2 (삭제 안됨)
        Feed feedToKeep2 = createFeed(user, weather, "유지될 피드 2", false, twoDaysAgo);

        // 삭제될 피드에 종속된 데이터 생성
        Comment comment = commentRepository.save(Comment.builder().feed(feedToDelete).author(user).content("댓글").build());
        FeedLike feedLike = feedLikeRepository.save(FeedLike.builder().feed(feedToDelete).user(user).build());

        // Job 파라미터 설정 (7일 전 기준)
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("deadline", Instant.now().minus(7, ChronoUnit.DAYS).toString())
            .toJobParameters();

        jobLauncherTestUtils.setJob(deleteSoftDeletedFeedsJob);

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 피드 삭제 여부 검증
        assertThat(feedRepository.findById(feedToDelete.getId())).isEmpty();
        assertThat(feedRepository.findById(feedToKeep1.getId())).isPresent();
        assertThat(feedRepository.findById(feedToKeep2.getId())).isPresent();

        // 종속 데이터 삭제 여부 검증
        assertThat(commentRepository.findById(comment.getId())).isEmpty();
        assertThat(feedLikeRepository.findById(feedLike.getId())).isEmpty();
    }

    private Feed createFeed(User user, Weather weather, String content, boolean isDeleted, Instant updatedAt) {
        Feed feed = Feed.builder()
            .author(user)
            .weather(weather)
            .content(content)
            .isDeleted(isDeleted)
            .build();
        Feed savedFeed = feedRepository.save(feed);

        // JPA Auditing 우회하고 updatedAt 직접 설정
        jdbcTemplate.update(
            "UPDATE feeds SET updated_at = ? WHERE id = ?",
            Timestamp.from(updatedAt),
            savedFeed.getId()
        );

        return feedRepository.findById(savedFeed.getId()).orElseThrow();
    }
}
