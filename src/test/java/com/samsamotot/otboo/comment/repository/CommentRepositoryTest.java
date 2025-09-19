package com.samsamotot.otboo.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.fixture.CommentFixture;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.samsamotot.otboo")
@Import({TestJpaAuditingConfig.class, QueryDslConfig.class})
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@DisplayName("Comment 레포지토리 슬라이스 테스트")
public class CommentRepositoryTest {

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
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager em;

    User author;
    Feed feed;

    @BeforeEach
    void setUp() {
        author = UserFixture.createUser();
        Location location = LocationFixture.createLocation();
        Weather weather = WeatherFixture.createWeather(location);
        feed = FeedFixture.createFeed(author, weather);
        em.persist(author);
        em.persist(location);
        em.persist(weather);
        em.persist(feed);

        em.flush();
        em.clear();
    }

    @Test
    void 첫_페이지_조회_시_최신순으로_댓글을_조회한다() {

        // given
        int limit = 5;
        for (int i = 0; i < 5; i ++) {
            Comment comment = CommentFixture.createComment(feed, author);
            em.persist(comment);
        }
        em.flush();
        em.clear();

        // when
        List<Comment> result = commentRepository.findByFeedIdWithCursor(
            feed.getId(),
            null,
            null,
            limit + 1
        );

        // then
        assertThat(result).hasSize(limit);
        assertThat(result).isSortedAccordingTo(
            Comparator.comparing(Comment::getCreatedAt).reversed()
                .thenComparing(Comment::getId, Comparator.reverseOrder())
        );
    }

    @Test
    void 커서가_주어졌을_때_이후_데이터만_조회된다() {

        // given
        int limit = 5;
        List<Comment> savedComments = new ArrayList<>();
        for (int i = 0; i < 5; i ++) {
            Comment comment = CommentFixture.createComment(feed, author);
            em.persist(comment);
            ReflectionTestUtils.setField(comment, "createdAt", Instant.now().plusSeconds(i));
            savedComments.add(comment);
        }
        em.flush();
        em.clear();

        Comment cursorComment = savedComments.get(2);
        String cursor = String.valueOf(cursorComment.getCreatedAt());
        UUID idAfter = cursorComment.getId();

        // when
        List<Comment> result = commentRepository.findByFeedIdWithCursor(
            feed.getId(),
            cursor,
            idAfter,
            limit + 1
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).isSortedAccordingTo(
            Comparator.comparing(Comment::getCreatedAt).reversed()
                .thenComparing(Comment::getId, Comparator.reverseOrder())
        );
    }

    @Test
    void 생성시각이_동일한_댓글이_있을_때_ID로_2차정렬하여_누락없이_조회한다() {

        // given
        Instant fixedTime = Instant.now();
        int totalComments = 5;
        List<Comment> savedComments = new ArrayList<>();

        for (int i = 0; i < totalComments; i++) {
            Comment comment = CommentFixture.createComment(feed, author);
            em.persist(comment);
            ReflectionTestUtils.setField(comment, "createdAt", fixedTime);
            savedComments.add(comment);
        }
        em.flush();
        em.clear();

        // 예상 결과 리스트 정렬 (id DESC)
        savedComments.sort(Comparator.comparing(Comment::getId, Comparator.reverseOrder()));

        Comment cursorComment = savedComments.get(2);
        String cursor = String.valueOf(cursorComment.getCreatedAt());
        UUID idAfter = cursorComment.getId();

        // when
        // 커서 다음부터 모든 댓글을 조회
        List<Comment> result = commentRepository.findByFeedIdWithCursor(
            feed.getId(),
            cursor,
            idAfter,
            totalComments
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(savedComments.get(3).getId());
        assertThat(result.get(1).getId()).isEqualTo(savedComments.get(4).getId());
    }
}
