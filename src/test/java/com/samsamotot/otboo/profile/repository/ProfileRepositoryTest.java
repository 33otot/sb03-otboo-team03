package com.samsamotot.otboo.profile.repository;

import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.profile.entity.Gender;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("Profile 레포지토리 슬라이스 테스트")
class ProfileRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TestEntityManager em;

    private Grid grid;
    private Location location;

    @BeforeEach
    void setUp() {
        grid = GridFixture.createGrid();
        em.persist(grid);

        location = Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("서울특별시", "중구"))
                .build();
        em.persist(location);

        em.flush();
    }

    @Test
    @DisplayName("findAllByLocationGridId - 특정 격자 ID에 속한 위치 정보를 갖는 모든 프로필을 조회한다")
    void findAllByLocationGridId_Success() {
        // given
        User user1 = UserFixture.createUser();
        User user2 = UserFixture.createUser();
        org.springframework.test.util.ReflectionTestUtils.setField(user2, "email", "testUser2@test.com");
        em.persist(user1);
        em.persist(user2);

        Location location2 = Location.builder()
                .latitude(35.1796)
                .longitude(129.0756)
                .grid(grid)
                .locationNames(List.of("부산광역시", "연제구"))
                .build();
        em.persist(location2);

        Profile profile1 = Profile.builder()
                .user(user1)
                .location(location)
                .name("사용자1")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 5, 5))
                .weatherNotificationEnabled(true)
                .build();
        Profile profile2 = Profile.builder()
                .user(user2)
                .location(location2)
                .name("사용자2")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1996, 6, 6))
                .weatherNotificationEnabled(true)
                .build();
        em.persist(profile1);
        em.persist(profile2);

        em.flush();
        em.clear();

        // when
        List<Profile> result = profileRepository.findAllByLocationGridId(grid.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Profile::getName).containsExactlyInAnyOrder("사용자1", "사용자2");
    }

    @Test
    @DisplayName("findByUserIdIn - 여러 사용자 ID 목록에 일치하는 프로필 목록을 일괄 조회한다")
    void findByUserIdIn_Success() {
        // given
        User user1 = UserFixture.createUser();
        User user2 = UserFixture.createUser();
        User user3 = UserFixture.createUser();
        org.springframework.test.util.ReflectionTestUtils.setField(user2, "email", "testUser2@test.com");
        org.springframework.test.util.ReflectionTestUtils.setField(user3, "email", "testUser3@test.com");
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);

        Location location2 = Location.builder()
                .latitude(35.1796)
                .longitude(129.0756)
                .grid(grid)
                .locationNames(List.of("부산광역시", "연제구"))
                .build();
        Location location3 = Location.builder()
                .latitude(36.3504)
                .longitude(127.3845)
                .grid(grid)
                .locationNames(List.of("대전광역시", "서구"))
                .build();
        em.persist(location2);
        em.persist(location3);

        Profile profile1 = Profile.builder()
                .user(user1)
                .location(location)
                .name("사용자1")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 5, 5))
                .weatherNotificationEnabled(true)
                .build();
        Profile profile2 = Profile.builder()
                .user(user2)
                .location(location2)
                .name("사용자2")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1996, 6, 6))
                .weatherNotificationEnabled(true)
                .build();
        Profile profile3 = Profile.builder()
                .user(user3)
                .location(location3)
                .name("사용자3")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1997, 7, 7))
                .weatherNotificationEnabled(true)
                .build();
        em.persist(profile1);
        em.persist(profile2);
        em.persist(profile3);

        em.flush();
        em.clear();

        // when
        List<Profile> result = profileRepository.findByUserIdIn(List.of(user1.getId(), user2.getId()));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Profile::getName).containsExactlyInAnyOrder("사용자1", "사용자2");
    }
}
