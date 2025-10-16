package com.samsamotot.otboo.comment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.repository.CommentRepository;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.config.TestConfig;
import com.samsamotot.otboo.feed.entity.Feed;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class, SecurityTestConfig.class})
@DirtiesContext(classMode = AFTER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@Sql(statements = {
    "TRUNCATE TABLE comments CASCADE",
    "TRUNCATE TABLE feeds CASCADE",
    "TRUNCATE TABLE clothes CASCADE",
    "TRUNCATE TABLE weathers CASCADE",
    "TRUNCATE TABLE grids CASCADE",
    "TRUNCATE TABLE users CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Comment 통합 테스트")
public class CommentIntegrationTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentRepository commentRepository;

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
                .content("댓글 테스트용 피드")
                .build()
        );
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {

        @Test
        @WithUserDetails(
            value = "testuser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 유효한_요청이면_댓글이_생성되고_commentCount가_증가한다() throws Exception {

            // given
            long before = feedRepository.findById(testFeed.getId()).orElseThrow().getCommentCount();

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(testUser.getId())
                .content("댓글 생성 테스트")
                .build();

            // when
            MvcResult result = mockMvc.perform(
                    post("/api/feeds/" + testFeed.getId() + "/comments")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

            // then
            Feed refreshed = feedRepository.findById(testFeed.getId()).orElseThrow();
            assertThat(refreshed.getCommentCount()).isEqualTo(before + 1);
        }

        @Test
        @WithUserDetails(value = "testuser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 내용이_비어있으면_400_에러를_반환한다() throws Exception {

            // given
            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(testUser.getId())
                .content("")
                .build();

            // when & then
            mockMvc.perform(
                    post("/api/feeds/" + testFeed.getId() + "/comments")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserDetails(value = "testuser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 존재하지_않는_피드에는_404_에러를_반환한다() throws Exception {

            // given
            UUID invalidFeedId = UUID.randomUUID();

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(testUser.getId())
                .content("댓글 생성 테스트")
                .build();

            mockMvc.perform(
                    post("/api/feeds/" + invalidFeedId + "/comments")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @BeforeEach
        void setUpComments() {
            // 댓글 3개 생성
            for (int i = 0; i < 3; i++) {
                commentRepository.save(Comment.builder()
                    .author(testUser)
                    .feed(testFeed)
                    .content("댓글 " + i)
                    .build());
            }
        }

        @Test
        @WithUserDetails(value = "testuser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 특정_피드의_댓글_목록을_조회한다() throws Exception {

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", testFeed.getId())
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.totalCount").value(3));
        }

        @Test
        @WithUserDetails(value = "testuser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 최신순_DESC_정렬이_적용된다() throws Exception {

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", testFeed.getId())
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("댓글 2"))
                .andExpect(jsonPath("$.data[1].content").value("댓글 1"))
                .andExpect(jsonPath("$.data[2].content").value("댓글 0"));
        }

        @Test
        @WithUserDetails(value = "testuser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void limit을_적용하고_totalCount에_전체_건수를_반환한다() throws Exception {

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", testFeed.getId())
                    .param("limit", "2")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(3));
        }

        @Test
        @WithUserDetails(value = "testuser@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
        void 존재하지_않는_피드의_댓글_목록_조회시_404를_반환한다() throws Exception {

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", java.util.UUID.randomUUID())
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name()))
                .andExpect(status().isNotFound());
        }
    }
}
