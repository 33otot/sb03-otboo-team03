# QueryDSL 리팩토링 분석 및 계획서
> **대상 도메인:** 날씨 (Weather), 위치 (Location), 프로필 (Profile)  
> **생성 브랜치:** `refactor/querydsl-weather-location-profile`

이 문서는 날씨, 위치, 프로필 도메인의 JPA 리포지토리를 분석하여 꼭 JPA가 필요한 경우를 제외하고, QueryDSL을 도입했을 때 얻을 수 있는 이점(타입 안정성, 가독성, 동적 쿼리 및 조인 최적화 등)이 있는 대상을 선별하고 이에 대한 리팩토링 계획을 정리한 문서입니다.

---

## 1. 리팩토링 및 쿼리 분석 결과 요약

### 1.1. 날씨 (Weather) 도메인
`WeatherRepository`에는 복잡한 `@Query` 및 긴 이름의 스프링 데이터 JPA 쿼리 메서드가 다수 존재합니다. QueryDSL 도입을 통해 명시적인 쿼리 표현과 타입 안정성 개선의 효과가 가장 큽니다.

* **`findAllByGrid(Grid grid)`**
  * **현재 방식:** `@Query("SELECT w FROM Weather w JOIN FETCH w.grid WHERE w.grid = :grid")`
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** Fetch Join을 사용하여 `grid`를 함께 조회하고 있습니다. QueryDSL을 사용하면 `join(weather.grid, grid).fetchJoin()` 형태로 작성하여 컴파일 시점에 타입 안정성을 보장받을 수 있습니다.
* **`findTopByGridAndForecastAtOrderByForecastedAtDesc(Grid grid, Instant forecastAt)`**
  * **현재 방식:** Spring Data JPA Query Method
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** 메서드 명칭이 지나치게 길어 가독성이 떨어집니다. QueryDSL을 사용하여 `.orderBy(weather.forecastedAt.desc()).limit(1)`로 명시적으로 나타내어 가독성을 크게 극대화할 수 있습니다.
* **`deleteOldAndUnreferencedWeather(Grid grid, Instant threshold)`**
  * **현재 방식:** `@Modifying` + JPQL Bulk Delete (`DELETE FROM Weather w WHERE w.grid = :grid AND w.forecastAt < :threshold AND NOT EXISTS (SELECT 1 FROM Feed f WHERE f.weather = w)`)
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** 서브쿼리를 포함하는 벌크 삭제 쿼리입니다. QueryDSL을 사용해 문자열 기반 JPQL을 제거하고 타입 안정성이 확보된 벌크 쿼리로 전환합니다. *단, 벌크 연산 후 영속성 컨텍스트 관리(EntityManager 초기화)에 유의하여 구현할 계획입니다.*
* **`deleteOutdatedAndUnreferencedWeather(Grid grid)`**
  * **현재 방식:** `@Modifying` + 중첩 `EXISTS` JPQL Bulk Delete
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** 동일 격자 내 오래된 발표 데이터를 삭제하기 위해 다수의 EXISTS 절이 사용된 복잡한 쿼리입니다. QueryDSL `JPAExpressions` 서브쿼리를 사용하여 가독성과 유지보수 편의성을 대폭 향상시킵니다.
* **`findByGridAndForecastedAtAndForecastAt(...)`**
  * **현재 방식:** Spring Data JPA Query Method
  * **QueryDSL 변환 여부:** **부적합 (N)**
  * **이유:** 단순 3개 컬럼 일치 조건의 단건 조회이므로, 스프링 데이터 JPA의 기본 쿼리 메서드 방식을 유지하는 것이 더 직관적입니다.

### 1.2. 위치 (Location) 도메인
`LocationRepository`는 격자(Grid)와의 연관 관계 조회 및 경/위도 기반 조회가 주를 이룹니다.

* **`findAllWithGrid()`**
  * **현재 방식:** `@Query("SELECT l FROM Location l JOIN FETCH l.grid")`
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** 배치 등에서 사용되는 N+1 문제 해결용 Fetch Join 쿼리입니다. 타입 안정성 확보를 위해 QueryDSL로 이관합니다.
* **`findByLongitudeAndLatitude(double longitude, double latitude)`**
  * **현재 방식:** `@Query` 기반 JPQL
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** JPQL 문자열로 작성된 조회를 QueryDSL 타입 세이프 쿼리로 이관하여 오타 방지 및 안전한 파라미터 바인딩을 보장받습니다.
* **`findGridByLongitudeAndLatitude(double longitude, double latitude)`**
  * **현재 방식:** `@Query("SELECT l.grid FROM Location l ...")`
  * **QueryDSL 변환 여부:** **보류 (N) - 미사용 쿼리**
  * **이유:** 리서치 결과, 메인 소스 코드 및 테스트 코드 전체에서 해당 메서드를 호출하는 곳이 없습니다. 리팩토링 단계에서 사용 여부를 검토하여 제거하거나, 필요한 경우에만 이관합니다.

### 1.3. 프로필 (Profile) 도메인
`ProfileRepository`는 유저와 위치, 격자 정보를 다단계 객체 그래프 탐색으로 필터링하는 쿼리가 존재합니다.

* **`findAllByLocationGridId(UUID gridId)`**
  * **현재 방식:** JPA Query Method (`profile.location.grid.id` 탐색)
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** `Profile` -> `Location` -> `Grid` 순으로 연관 관계를 타고 들어가는 다단계 Property Path 쿼리입니다. 암시적으로 조인이 다수 발생하며, 조인 성능 최적화를 관리하기 어렵습니다. QueryDSL을 사용하여 명시적으로 `join` 구조를 표현하여 가독성과 쿼리 예측 가능성을 높입니다.
* **`findByUserIdIn(Collection<UUID> userIds)`**
  * **현재 방식:** `@Query("select p from Profile p where p.user.id in :userIds")`
  * **QueryDSL 변환 여부:** **적합 (Y)**
  * **이유:** `IN` 조건이 적용된 JPQL 쿼리입니다. QueryDSL을 사용하여 `profile.user.id.in(userIds)`로 보다 깔끔하고 안전하게 처리합니다.
* **`findByUserId(UUID userId)`**
  * **현재 방식:** JPA Query Method
  * **QueryDSL 변환 여부:** **부적합 (N)**
  * **이유:** 단일 외래키에 기반한 단순 단건 조회이므로 기존 JPA 기능을 그대로 사용합니다.

---

## 2. 패키지 및 구조 설계 (컨벤션 준수)

기존 `feed` 도메인에 정의된 QueryDSL 관례(`FeedRepositoryCustom` / `FeedRepositoryImpl`)를 준수하여 구현합니다.

### 2.1. 패키지 구조 계획
```
src/main/java/com/samsamotot/otboo/
├── weather/repository/
│   ├── WeatherRepository.java (기존 JPA Repository + Custom 상속)
│   ├── WeatherRepositoryCustom.java (QueryDSL 인터페이스)
│   └── WeatherRepositoryImpl.java (QueryDSL 구현체, JPAQueryFactory 사용)
├── location/repository/
│   ├── LocationRepository.java (기존 JPA Repository + Custom 상속)
│   ├── LocationRepositoryCustom.java (QueryDSL 인터페이스)
│   └── LocationRepositoryImpl.java (QueryDSL 구현체, JPAQueryFactory 사용)
└── profile/repository/
    ├── ProfileRepository.java (기존 JPA Repository + Custom 상속)
    ├── ProfileRepositoryCustom.java (QueryDSL 인터페이스)
    └── ProfileRepositoryImpl.java (QueryDSL 구현체, JPAQueryFactory 사용)
```

### 2.2. 구현 예시 가이드라인
기존 Feed 도메인의 방식과 같이 `JPAQueryFactory`와 각 도메인의 `QClass` 인스턴스를 필드로 구성하여 작성합니다.

```java
// 예시: LocationRepositoryImpl 구현 가이드
@Repository
@RequiredArgsConstructor
public class LocationRepositoryImpl implements LocationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QLocation qLocation = QLocation.location;

    @Override
    public Optional<Location> findByLongitudeAndLatitude(double longitude, double latitude) {
        return Optional.ofNullable(
            queryFactory
                .selectFrom(qLocation)
                .where(qLocation.longitude.eq(longitude)
                    .and(qLocation.latitude.eq(latitude)))
                .fetchOne()
        );
    }
}
```

---

## 3. 리팩토링 구현 단계 계획

유저 컨펌 후 구현 단계로 진입 시 다음과 같이 차례대로 안전하게 진행합니다.

1. **QueryDSL Q클래스 재생성 및 확인**
   * Gradle 컴파일(`compileJava`)을 수행하여 엔티티 변경 사항이 반영된 최신 Q클래스들을 빌드 디렉토리에 생성합니다.
2. **도메인별 리팩토링 수행 (날씨 -> 위치 -> 프로필)**
   * `Custom` 인터페이스 작성 및 `Impl` 클래스 구현.
   * 기존 `Repository`에 `Custom` 상속 추가 및 `@Query`/메서드 쿼리 제거.
3. **비즈니스 레이어 검증**
   * 리포지토리 변경에 따른 서비스 레이어 코드 영향이 없는지 빌드 검증을 수행합니다.
4. **테스트 코드 검증 및 빌드**
   * 기존 통합 및 단위 테스트를 수행하여 기능 오작동이 없는지 최종 검토합니다.
