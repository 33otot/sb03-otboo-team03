package com.samsamotot.otboo.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.fixture.ClothesFixture;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.config.TestConfig;
import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.FeedUpdateRequest;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.service.FeedDataSyncService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.GridRepository;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.http.MediaType;
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
import org.testcontainers.elasticsearch.ElasticsearchContainer;
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
    "TRUNCATE TABLE feed_clothes CASCADE",
    "TRUNCATE TABLE feeds CASCADE",
    "TRUNCATE TABLE clothes CASCADE",
    "TRUNCATE TABLE weathers CASCADE",
    "TRUNCATE TABLE grids CASCADE",
    "TRUNCATE TABLE users CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Feed 통합 테스트")
public class FeedIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static ElasticsearchContainer es =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.14.0")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withCommand("sh", "-c",
                "bin/elasticsearch-plugin install analysis-nori --batch && exec /usr/local/bin/docker-entrypoint.sh");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
        // Elasticsearch 설정
        reg.add("spring.elasticsearch.uris", es::getHttpHostAddress);
        reg.add("spring.data.elasticsearch.index-prefix", () -> "test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeedDataSyncService feedDataSyncService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private ClothesRepository clothesRepository;

    @Autowired
    private ElasticsearchOperations operations;

    private User testUser;
    private User otherUser;
    private Weather testWeather;
    private List<Clothes> testClothes;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(UserFixture.createUserWithEmail("testuser@example.com"));
        otherUser = userRepository.save(UserFixture.createUserWithEmail("otheruser@example.com"));
        Grid grid = gridRepository.save(GridFixture.createGrid());
        testWeather = weatherRepository.save(WeatherFixture.createWeather(grid));
        testClothes = clothesRepository.saveAll(
            IntStream.range(0, 3)
                .mapToObj(i -> ClothesFixture.createClothes(testUser))
                .collect(Collectors.toList())
        );
    }

    @BeforeEach
    void bootstrapIndex() {
        var indexOps = operations.indexOps(FeedDocument.class);

        if (indexOps.exists()) indexOps.delete();

        // @Setting/@Mapping에 연결된 파일을 이용해 생성
        var settings = Document.from(indexOps.createSettings());
        var mapping  = indexOps.createMapping(FeedDocument.class);

        if (settings != null && !settings.isEmpty()) indexOps.create(settings);
        else indexOps.create();

        if (mapping != null && !mapping.isEmpty()) indexOps.putMapping(mapping);
    }

    @AfterEach
    void tearDown() {
        IndexOperations indexOps = operations.indexOps(FeedDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();   // 인덱스 전체 삭제
        }
    }

    @Nested
    @DisplayName("피드 생성 테스트")
    class CreateFeed {

        @Test
        @WithUserDetails(
            value = "testuser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 유효한_요청_시_피드가_생성된다() throws Exception {
            // given
            List<UUID> clothesIds = testClothes.stream().map(Clothes::getId).collect(Collectors.toList());
            FeedCreateRequest request = FeedCreateRequest.builder()
                .content("오늘의 OOTD입니다!")
                .authorId(testUser.getId())
                .weatherId(testWeather.getId())
                .clothesIds(clothesIds)
                .build();

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("오늘의 OOTD입니다!"))
                .andExpect(jsonPath("$.author.userId").value(testUser.getId().toString()))
                .andDo(print());
        }

        @Test
        @WithUserDetails(
            value = "testuser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 내용이_없으면_400_에러를_반환한다() throws Exception {
            // given
            FeedCreateRequest request = FeedCreateRequest.builder()
                .content("")
                .authorId(testUser.getId())
                .weatherId(testWeather.getId())
                .clothesIds(List.of())
                .build();

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("피드 목록 조회 테스트")
    class GetFeeds {

        @BeforeEach
        void setUp() {
            // 테스트용 피드 5개 생성 (Elasticsearch 동기화)
            for (int i = 0; i < 5; i++) {
                Feed feed = feedRepository.save(
                    Feed.builder()
                        .author(testUser)
                        .weather(testWeather)
                        .content("피드 " + i)
                        .build()
                );
                FeedDto feedDto = FeedFixture.createFeedDto(feed);
                feedDataSyncService.syncFeedToElasticsearch(feedDto);
            }

            Feed deletedFeed = feedRepository.save(Feed.builder()
                .author(testUser)
                .weather(testWeather)
                .content("삭제된 피드")
                .build());
            FeedDto deletedFeedDto = FeedFixture.createFeedDto(deletedFeed);
            feedDataSyncService.syncFeedToElasticsearch(deletedFeedDto);
            feedDataSyncService.softDeleteFeedFromElasticsearch(deletedFeed.getId());
        }

        @Test
        @WithUserDetails(
            value = "testuser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 피드_목록을_조회하고_이때_삭제된_피드는_포함하지_않는다() throws Exception {
            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.data[?(@.content == '삭제된 피드')]").doesNotExist())
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("피드 수정 테스트")
    class UpdateFeed {

        private Feed userFeed;

        @BeforeEach
        void setUp() {
            userFeed = feedRepository.save(
                Feed.builder()
                    .author(testUser)
                    .weather(testWeather)
                    .content("수정 전 내용")
                    .build()
            );
        }

        @Test
        @WithUserDetails(
            value = "testuser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 자신이_작성한_피드를_수정한다() throws Exception {
            // given
            FeedUpdateRequest request = new FeedUpdateRequest("수정된 내용입니다.");

            // when & then
            mockMvc.perform(patch("/api/feeds/" + userFeed.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 내용입니다."))
                .andDo(print());
        }

        @Test
        @WithUserDetails(
            value = "otheruser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 다른_사람이_작성한_피드는_수정할_수_없다() throws Exception {
            // given
            FeedUpdateRequest request = new FeedUpdateRequest("수정 시도");

            // when & then
            mockMvc.perform(patch("/api/feeds/" + userFeed.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("피드 삭제 테스트")
    class DeleteFeed {

        private Feed userFeed;

        @BeforeEach
        void setUp() {
            userFeed = feedRepository.save(
                Feed.builder()
                    .author(testUser)
                    .weather(testWeather)
                    .content("삭제될 피드")
                    .build()
            );
        }

        @Test
        @WithUserDetails(
            value = "testuser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 자신이_작성한_피드를_삭제한다() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/feeds/" + userFeed.getId())
                    .with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(print());

            Feed deletedFeed = feedRepository.findById(userFeed.getId()).orElseThrow();
            assertThat(deletedFeed.isDeleted()).isTrue();
        }

        @Test
        @WithUserDetails(
            value = "otheruser@example.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
        )
        void 다른_사람이_작성한_피드는_삭제할_수_없다() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/feeds/" + userFeed.getId())
                    .with(csrf()))
                .andExpect(status().isForbidden())
                .andDo(print());
        }
    }
}
