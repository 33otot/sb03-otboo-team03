package com.samsamotot.otboo.follow.repository;

import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
import com.samsamotot.otboo.follow.entity.Follow;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * PackageName  : com.samsamotot.otboo.follow.repository
 * FileName     : FollowRepositoryImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({FollowRepositoryImpl.class, QueryDslConfig.class})
@DisplayName("팔로우 레포지토리 QueryDSL 슬라이스 테스트")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class FollowRepositoryImplTest {
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
    private FollowRepository followRepository;

    @Autowired
    private EntityManager em;

    private void insertUser(UUID id, String email, String username, boolean locked, Instant createdAt) {
        em.createNativeQuery("""
        INSERT INTO users (id, email, username, password, provider, provider_id, role, is_locked, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """)
            .setParameter(1, id)
            .setParameter(2, email)
            .setParameter(3, username)
            .setParameter(4, "{noop}pw")
            .setParameter(5, "LOCAL")
            .setParameter(6, null)
            .setParameter(7, "USER")
            .setParameter(8, locked)
            .setParameter(9, Timestamp.from(createdAt))
            .executeUpdate();
    }

    private void insertFollow(UUID id, UUID followerId, UUID followeeId, Instant createdAt) {
        em.createNativeQuery("""
            INSERT INTO follows (id, follower_id, followee_id, created_at)
            VALUES (?, ?, ?, ?)
        """)
            .setParameter(1, id)
            .setParameter(2, followerId)
            .setParameter(3, followeeId)
            .setParameter(4, Timestamp.from(createdAt))
            .executeUpdate();
    }

    private FollowingRequest req(UUID followerId, Instant cursorCreatedAt, UUID idAfter, int limit, String nameLike) {
        String cursor = cursorCreatedAt == null ? null : cursorCreatedAt.toString();
        return new FollowingRequest(followerId, cursor, idAfter, limit, nameLike);
    }

    @Test
    void 팔로우_목록을_DESC로_정렬해_응답한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        Instant t1 = Instant.parse("2025-09-15T09:00:00Z");
        Instant t2 = Instant.parse("2025-09-15T10:00:00Z");
        Instant t3 = Instant.parse("2025-09-15T11:00:00Z");

        insertUser(follower, "f@example.com", "f", false, t1);
        insertUser(u1, "1@example.com", "a1", false, t1);
        insertUser(u2, "2@example.com", "a2", false, t1);
        insertUser(u3, "3@example.com", "a3", false, t1);

        insertFollow(UUID.randomUUID(), follower, u1, t1);
        insertFollow(UUID.randomUUID(), follower, u2, t2);
        insertFollow(UUID.randomUUID(), follower, u3, t3);

        em.flush(); em.clear();

        // when
        List<Follow> rows = followRepository.findFollowings(req(follower, null, null, 10, null));

        assertThat(rows).isNotEmpty();
        assertThat(rows.stream().map(Follow::getCreatedAt).toList())
            .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void limit_만큼의_응답을_반환합니다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();
        Instant base = Instant.parse("2025-09-15T12:00:00Z");

        insertUser(follower, "ff@example.com", "ff", false, base);
        insertUser(u1, "1@example.com", "u1", false, base);
        insertUser(u2, "2@example.com", "u2", false, base);
        insertUser(u3, "3@example.com", "u3", false, base);

        insertFollow(UUID.randomUUID(), follower, u1, base.plusSeconds(10));
        insertFollow(UUID.randomUUID(), follower, u2, base.plusSeconds(20));
        insertFollow(UUID.randomUUID(), follower, u3, base.plusSeconds(30));

        em.flush(); em.clear();

        int limit = 2;

        List<Follow> rows = followRepository.findFollowings(req(follower, null, null, limit, null));

        assertThat(rows.size()).isBetween(Math.min(1, limit), limit + 1);
    }

    @Test
    void 팔로우_생성_기준으로_정렬한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID uOld = UUID.randomUUID();
        UUID uNew = UUID.randomUUID();
        Instant tOld = Instant.parse("2025-09-15T08:00:00Z");
        Instant tNew = Instant.parse("2025-09-15T13:00:00Z");

        insertUser(follower, "a@example.com", "a", false, tOld);
        insertUser(uOld, "old@example.com", "old", false, tOld);
        insertUser(uNew, "new@example.com", "new", false, tOld);

        insertFollow(UUID.randomUUID(), follower, uNew, tNew); // 최신
        insertFollow(UUID.randomUUID(), follower, uOld, tOld); // 과거

        em.flush(); em.clear();

        // when
        List<Follow> rows = followRepository.findFollowings(req(follower, null, null, 10, null));

        // then
        Instant maxCreatedAt = rows.stream().map(Follow::getCreatedAt).max(Comparator.naturalOrder()).orElse(tOld);
        assertThat(rows.get(0).getCreatedAt()).isEqualTo(maxCreatedAt);
    }


    @Test
    void 같은_커서값을_가진_객체가_여러개일_경우_보조커서_기준으로_조회한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        Instant tie = Instant.parse("2025-09-15T14:00:00Z");
        Instant older = Instant.parse("2025-09-15T13:00:00Z");

        insertUser(follower, "c0@example.com", "c0", false, older);
        insertUser(u1, "c1@example.com", "x1", false, older);
        insertUser(u2, "c2@example.com", "x2", false, older);
        insertUser(u3, "c3@example.com", "x3", false, older);

        UUID idTieA = UUID.randomUUID();
        UUID idTieB = UUID.randomUUID();
        UUID idOlder = UUID.randomUUID();

        insertFollow(idTieA, follower, u1, tie);
        insertFollow(idTieB, follower, u2, tie);
        insertFollow(idOlder, follower, u3, older);

        em.flush(); em.clear();

        // when
        List<Follow> rows = followRepository.findFollowings(
            req(follower, tie, idTieA, 10, null)
        );

        // then
        assertThat(rows).extracting(Follow::getId).doesNotContain(idTieA);
        assertThat(rows).allSatisfy(f -> {
            assertThat(f.getCreatedAt()).isBeforeOrEqualTo(tie);
        });

        assertThat(rows.stream().map(Follow::getCreatedAt).toList())
            .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void nameLike_조건이_있으면_일치하는_값을_반환한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID ukim1 = UUID.randomUUID();
        UUID uetc  = UUID.randomUUID();
        UUID ukim2 = UUID.randomUUID();

        Instant base = Instant.parse("2025-09-15T15:00:00Z");
        insertUser(follower, "fkim@example.com", "fkim", false, base);
        insertUser(ukim1, "k1@example.com", "KimSun", false, base);
        insertUser(uetc , "e2@example.com", "Park", false, base);
        insertUser(ukim2, "k3@example.com", "vintageKIM", false, base);

        insertFollow(UUID.randomUUID(), follower, ukim1, base.plusSeconds(10));
        insertFollow(UUID.randomUUID(), follower, uetc , base.plusSeconds(20));
        insertFollow(UUID.randomUUID(), follower, ukim2, base.plusSeconds(30));

        em.flush(); em.clear();

        // when
        List<Follow> rows = followRepository.findFollowings(req(follower, null, null, 10, "kim"));

        // then:
        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(f ->
            assertThat(f.getFollowee().getUsername().toLowerCase()).contains("kim")
        );
    }

    @Test
    void 조건이_없으면_모든_팔로잉_유저를_반환한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        Instant base = Instant.parse("2025-09-15T16:00:00Z");

        insertUser(follower, "e0@example.com", "e0", false, base);
        insertUser(u1, "e1@example.com", "e1", false, base);
        insertUser(u2, "e2@example.com", "e2", false, base);

        insertFollow(UUID.randomUUID(), follower, u1, base.plusSeconds(10));
        insertFollow(UUID.randomUUID(), follower, u2, base.plusSeconds(20));

        em.flush(); em.clear();

        // when
        List<Follow> rows = followRepository.findFollowings(req(follower, null, null, 100, null));

        // then
        assertThat(rows.size()).isGreaterThanOrEqualTo(2);
        assertThat(rows).extracting(Follow::getFollower).extracting("id").contains(follower);
    }

    @Test
    void nameLike_조건에_맞는_수를_카운트한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        Instant base = Instant.parse("2025-09-15T17:00:00Z");

        UUID u1 = UUID.randomUUID(); // 매칭
        UUID u2 = UUID.randomUUID(); // 매칭
        UUID u3 = UUID.randomUUID(); // 매칭(x)

        insertUser(follower, "f@example.com", "f", false, base);
        insertUser(u1, "1@example.com", "테스트A", false, base);
        insertUser(u2, "2@example.com", "ZZ테스트zz", false, base);
        insertUser(u3, "3@example.com", "nothing", false, base);

        insertFollow(UUID.randomUUID(), follower, u1, base.plusSeconds(10));
        insertFollow(UUID.randomUUID(), follower, u2, base.plusSeconds(20));
        insertFollow(UUID.randomUUID(), follower, u3, base.plusSeconds(30));

        em.flush(); em.clear();

        // when
        long total = followRepository.countTotalElements(follower, null);
        long likeCount = followRepository.countTotalElements(follower, "테스트");

        assertThat(total).isGreaterThanOrEqualTo(3);
        assertThat(likeCount).isGreaterThan(0);
        assertThat(likeCount).isLessThanOrEqualTo(total);
    }

    @Test
    void 커서_문자열_파싱을_정상적으로_한다() throws Exception {
        // given
        UUID follower = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();
        UUID u4 = UUID.randomUUID();

        Instant t9  = Instant.parse("2025-09-15T09:00:00Z");
        Instant t10 = Instant.parse("2025-09-15T10:00:00Z");
        Instant t11 = Instant.parse("2025-09-15T11:00:00Z");
        Instant t12 = Instant.parse("2025-09-15T12:00:00Z");

        insertUser(follower, "f@example.com", "f", false, t9);
        insertUser(u1, "1@example.com", "a1", false, t9);
        insertUser(u2, "2@example.com", "a2", false, t9);
        insertUser(u3, "3@example.com", "a3", false, t9);
        insertUser(u4, "4@example.com", "a4", false, t9);

        insertFollow(UUID.randomUUID(), follower, u1, t9);
        insertFollow(UUID.randomUUID(), follower, u2, t10);
        insertFollow(UUID.randomUUID(), follower, u3, t11);
        insertFollow(UUID.randomUUID(), follower, u4, t12);

        em.flush(); em.clear();

        List<Follow> r1 = followRepository.findFollowings(
            req(follower, t11, null, 100, null) // cursor만, idAfter는 없음 → createdAt < cursor
        );
        assertThat(r1).allSatisfy(f -> assertThat(f.getCreatedAt()).isBefore(t11));

        Instant sameAsT11ViaKST = Instant.parse("2025-09-15T11:00:00Z"); // 비교용
        String offsetCursor = "2025-09-15T20:00:00+09:00";
        List<Follow> r2 = followRepository.findFollowings(
            new FollowingRequest(follower, offsetCursor, null, 100, null)
        );
        assertThat(r2).allSatisfy(f -> assertThat(f.getCreatedAt()).isBefore(sameAsT11ViaKST));

        String localCursor = "2025-09-15T11:00:00";
        List<Follow> r3 = followRepository.findFollowings(
            new FollowingRequest(follower, localCursor, null, 100, null)
        );
        assertThat(r3).allSatisfy(f -> assertThat(f.getCreatedAt()).isBefore(sameAsT11ViaKST));

        assertThat(r1).isNotEmpty();
        assertThat(r2).isNotEmpty();
        assertThat(r3).isNotEmpty();
        assertThat(r1).noneMatch(f -> !f.getCreatedAt().isBefore(t11));
        assertThat(r2).noneMatch(f -> !f.getCreatedAt().isBefore(t11));
        assertThat(r3).noneMatch(f -> !f.getCreatedAt().isBefore(t11));
    }
}