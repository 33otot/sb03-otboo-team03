package com.samsamotot.otboo.clothes.repository;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.user.entity.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
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
@DisplayName("의상 레포지토리 슬라이스 테스트")
public class ClothesRepositoryCustomTest {
    private static final Instant baseTime = Instant.parse("2024-09-01T00:00:00Z");
    private static final Logger log = LoggerFactory.getLogger(ClothesRepositoryCustomTest.class);

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
    private ClothesRepository clothesRepository;

    @Autowired
    private TestEntityManager em;

    User owner;

    @BeforeEach
    void setUp() {
        owner = UserFixture.createUser();
        em.persist(owner);

        em.flush();
        em.clear();
    }

    private void persistClothes(User owner, String name, String imageUrl, ClothesType type, Instant createdAt) {
        Clothes clothes = Clothes.builder()
            .owner(owner)
            .name(name)
            .imageUrl(imageUrl)
            .type(type)
            .build();

        em.persist(clothes);
        em.flush();

        // 직접 update 쿼리를 통해 createdAt 수정
        em.getEntityManager().createQuery("update Clothes c set c.createdAt = :createdAt where c.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", clothes.getId())
            .executeUpdate();
    }

    private ClothesSearchRequest req(UUID ownerId, ClothesType type, Instant cursor, int limit, UUID idAfter) {
        return ClothesSearchRequest.builder()
            .ownerId(ownerId)
            .typeEqual(type)
            .cursor(cursor != null ? cursor.toString() : null)
            .idAfter(idAfter)
            .limit(limit)
            .build();
    }

    @Nested
    @DisplayName("findClothesWithCursor 테스트")
    class FindClothesWithCursorTest {

        @Test
        void 의상_목록을_DESC_정렬하여_반환한다() {

            for (int i = 0; i < 4; i++) {
                persistClothes(owner, "반팔" + i, null, ClothesType.TOP, baseTime.plusSeconds(i));
            }

            for (int i = 10; i < 15; i++) {
                persistClothes(owner, "바지" + i, null, ClothesType.BOTTOM, baseTime.plusSeconds(i));
            }

            em.flush();
            em.clear();

            // when
            Slice<Clothes> slice = clothesRepository.findClothesWithCursor(
                owner.getId(),
                req(UUID.randomUUID(), null, null, 10, null),
                PageRequest.of(0, 10)
            );

            // then
            assertThat(slice.getContent()).hasSize(9);
            assertThat(slice.getContent())
                .extracting(Clothes::getCreatedAt)
                .isSortedAccordingTo(Comparator.reverseOrder());
        }

        @Test
        void 커서가_있다면_다음_데이터를_반환한다() {

            for (int i = 0; i < 4; i++) {
                persistClothes(owner, "반팔" + i, null, ClothesType.TOP, baseTime.plusSeconds(i));
            }

            em.flush();
            em.clear();

            // when: 커서를 3초짜리로 줌 → 0,1,2초 데이터 조회
            Slice<Clothes> slice = clothesRepository.findClothesWithCursor(
                owner.getId(),
                req(UUID.randomUUID(), null, baseTime.plusSeconds(3), 10, null),
                PageRequest.of(0, 10)
            );

            // then
            assertThat(slice.getContent()).hasSize(3);
            assertThat(slice.getContent())
                .extracting(Clothes::getCreatedAt)
                .containsExactlyInAnyOrder(
                    baseTime,
                    baseTime.plusSeconds(1),
                    baseTime.plusSeconds(2)
                );
        }

        @Test
        void 의상_타입이_조건으로_주어진다면_일치하는_의상만_반환되어야_한다() {

            for (int i = 0; i < 2; i++) {
                persistClothes(owner, "반팔" + i, null, ClothesType.TOP, baseTime.plusSeconds(i));
            }

            for (int i = 0; i < 5; i++) {
                persistClothes(owner, "바지" + i, null, ClothesType.BOTTOM, baseTime.plusSeconds(i));
            }

            em.flush();
            em.clear();

            // when
            Slice<Clothes> slice = clothesRepository.findClothesWithCursor(
                owner.getId(),
                req(UUID.randomUUID(), ClothesType.BOTTOM, baseTime.plusSeconds(3), 10, null),
                PageRequest.of(0, 10)
            );

            // then
            assertThat(slice.getContent()).hasSize(3);
            assertThat(slice.getContent())
                .extracting(Clothes::getType)
                .containsOnly(ClothesType.BOTTOM);

            assertThat(slice.getContent())
                .extracting(Clothes::getCreatedAt)
                .containsExactlyInAnyOrder(
                    baseTime,
                    baseTime.plusSeconds(1),
                    baseTime.plusSeconds(2)
                );
        }

    }

    @Nested
    @DisplayName("totalElement 테스트")
    class TotalElementTest {

        @Test
        void 조건없는_요청의_경우_조회된_전체_목록의_개수가_totalElement값이_되어야_한다() {

            for (int i = 0; i < 5; i++) {
                persistClothes(owner, "바지" + i, null, ClothesType.BOTTOM, baseTime.plusSeconds(i));
            }

            em.flush();
            em.clear();

            long totalElement = clothesRepository.totalElementCount(
                owner.getId(),
                req(UUID.randomUUID(), null, null, 10, null)
            );

            // then
            assertThat(totalElement).isEqualTo(5);
        }

        @Test
        void 조건으로_조회된_의상_목록의_개수가_totalElement값이_되어야_한다() {

            for (int i = 0; i < 2; i++) {
                persistClothes(owner, "반팔" + i, null, ClothesType.TOP, baseTime.plusSeconds(i));
            }

            for (int i = 0; i < 5; i++) {
                persistClothes(owner, "바지" + i, null, ClothesType.BOTTOM, baseTime.plusSeconds(i));
            }

            em.flush();
            em.clear();

            long totalElement = clothesRepository.totalElementCount(
                owner.getId(),
                req(UUID.randomUUID(), ClothesType.TOP, null, 10, null)
            );

            // then
            assertThat(totalElement).isEqualTo(2);
        }
    }

}
