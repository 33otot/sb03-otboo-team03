package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.FeedUpdateRequest;
import com.samsamotot.otboo.feed.dto.event.FeedDeleteEvent;
import com.samsamotot.otboo.feed.dto.event.FeedSyncEvent;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedLikeRepository;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.repository.FeedSearchRepository;
import com.samsamotot.otboo.notification.dto.event.FeedCreatedEvent;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드(Feed) 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedServiceImpl implements FeedService {

    private static final String SORT_BY_CREATED_AT = "createdAt";
    private static final String SORT_BY_LIKE_COUNT = "likeCount";
    private static final String SERVICE = "[FeedServiceImpl] ";

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedSearchRepository feedSearchRepository;
    private final FeedMapper feedMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final FeedDataSyncService feedDataSyncService;

    /**
     * 새로운 피드를 생성하고 저장합니다.
     * 요청 DTO를 기반으로 작성자, 날씨, 의상 정보를 조회하여
     * 피드 엔티티를 구성하고 데이터베이스에 저장합니다.
     *
     * @param feedCreateRequest 피드 생성에 필요한 데이터
     * @return  생성된 피드의 정보를 담은 FeedDto
     * @throws OtbooException 존재하지 않는 사용자/날씨 정보/의상 정보
     */
    @Override
    public FeedDto create(FeedCreateRequest feedCreateRequest) {

        UUID authorId = feedCreateRequest.authorId();
        UUID weatherId = feedCreateRequest.weatherId();
        List<UUID> clothesIds = feedCreateRequest.clothesIds();
        String content = feedCreateRequest.content();

        log.debug(SERVICE + "피드 등록 시작: authorId = {}, weatherId = {}, clothesIds = {}", authorId, weatherId, clothesIds);

        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND, Map.of("authorId", authorId.toString())));

        Weather weather = weatherRepository.findById(weatherId)
                .orElseThrow(() -> new OtbooException(ErrorCode.WEATHER_NOT_FOUND, Map.of("weatherId", weatherId.toString())));

        Set<UUID> uniqueClothesIds = new HashSet<>(clothesIds);
        List<Clothes> clothesList = clothesRepository.findAllById(uniqueClothesIds);

        // 조회된 의류 개수가 고유 ID 개수와 다르면, 존재하지 않는 ID가 있다는 의미
        if (clothesList.size() != uniqueClothesIds.size()) {

            Set<UUID> foundIds = clothesList.stream()
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());

            uniqueClothesIds.removeAll(foundIds);

            throw new OtbooException(
                ErrorCode.CLOTHES_NOT_FOUND,
                Map.of("notFoundClothesIds", uniqueClothesIds.toString())
            );
        }

        Feed feed = Feed.builder()
            .author(author)
            .weather(weather)
            .content(content)
            .build();

        clothesList.forEach(feed::addClothes);

        Feed saved = feedRepository.save(feed);
        FeedDto result = feedMapper.toDto(saved);

        // Elasticsearch 동기화
        eventPublisher.publishEvent(new FeedSyncEvent(result));
        log.debug(SERVICE + "피드 등록 완료: feedId = {}", saved.getId());

        eventPublisher.publishEvent(new FeedCreatedEvent(author));

        log.debug(SERVICE + "알림 등록 완료: feedId = {}", saved.getId());

        return result;
    }

    /**
     * 피드 목록을 커서 기반 페이징으로 조회합니다.
     * 다양한 검색 조건 및 정렬 조건을 사용하여 피드를 검색하고,
     * 현재 사용자의 좋아요 여부를 포함한 FeedDto 목록을 반환합니다.
     *
     * @param request 커서, 정렬 기준/방향, limit 크기, 검색 조건 등 페이징 요청 DTO
     * @param userId 현재 사용자의 ID
     * @return 조회된 피드 목록, 다음 커서 정보, 전체 개수 등을 포함하는 CursorResponse<FeedDto>
     */
    @Override
    @Transactional(readOnly = true)
    public CursorResponse<FeedDto> getFeeds(@Valid FeedCursorRequest request, UUID userId) {

        log.debug(SERVICE + "피드 목록 조회 시작: request = {}, userId = {}", request, userId);

        String cursor = request.cursor();
        UUID idAfter = request.idAfter();
        Integer limit = request.limit();
        String sortBy = request.sortBy();
        SortDirection sortDirection = request.sortDirection();
        String keywordLike = request.keywordLike();
        SkyStatus skyStatusEqual = request.skyStatusEqual();
        Precipitation precipitationTypeEqual = request.precipitationTypeEqual();
        UUID authorIdEqual = request.authorIdEqual();

        validateCursorRequest(cursor, sortBy);

        CursorResponse<FeedDto> result = feedSearchRepository.findByCursor(
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            keywordLike,
            skyStatusEqual,
            precipitationTypeEqual,
            authorIdEqual
        );

        log.info(SERVICE + "피드 목록 조회 완료 - 조회된 피드 수:{}, hasNext: {}", result.data().size(), result.hasNext());
        return result;
    }

    /**
     * 피드의 내용을 수정합니다.
     * 수정을 요청한 사용자가 피드의 작성자인지 확인합니다.
     *
     * @param feedId 수정할 피드의 ID
     * @param userId 현재 사용자의 ID
     * @param request 피드 수정 요청 DTO
     * @return 수정된 피드 DTO
     */
    @Override
    public FeedDto update(UUID feedId, UUID userId, FeedUpdateRequest request) {

        log.debug(SERVICE + "피드 수정 시작: feedId = {}, userId = {}, request = {}", feedId, userId, request);

        Feed feed = feedRepository.findByIdAndIsDeletedFalse(feedId)
            .orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", feedId.toString())));

        if (!feed.getAuthor().getId().equals(userId)) {
            throw new OtbooException(ErrorCode.FORBIDDEN_FEED_MODIFICATION,
                Map.of("feedId", feedId.toString(), "userId", userId.toString()));
        }

        String newContent = request.content();
        feed.updateContent(newContent);
        log.debug(SERVICE + "피드 수정 완료: feedId = {}, userId = {}", feedId, userId);
        FeedDto result = convertToDto(feed, userId);

        // Elasticsearch 동기화
        eventPublisher.publishEvent(new FeedSyncEvent(result));

        return result;
    }

    /**
     * 특정 피드를 삭제 처리합니다.
     * 삭제를 요청한 사용자가 피드의 작성자인지 확인 후, 피드를 논리적으로 삭제 처리합니다.
     *
     * @param feedId 삭제할 피드의 ID
     * @param userId 현재 사용자의 ID
     * @return 삭제 처리된 Feed 객체
     * @throws OtbooException 피드를 찾을 수 없는 경우 (FEED_NOT_FOUND)
     *                        삭제 권한이 없는 경우 (FORBIDDEN_FEED_DELETION)
     */
    @Override
    public Feed delete(UUID feedId, UUID userId) {

        log.debug(SERVICE + "피드 삭제 시작: feedId = {}, userId = {}", feedId, userId);

        Feed feed = feedRepository.findByIdAndIsDeletedFalse(feedId)
            .orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", feedId.toString())));

        if (!feed.getAuthor().getId().equals(userId)) {
            throw new OtbooException(ErrorCode.FORBIDDEN_FEED_DELETION, Map.of("feedId", feedId.toString()));
        }

        feed.delete();
        // Elasticsearch에서 소프트 삭제 처리
        eventPublisher.publishEvent(new FeedDeleteEvent(feedId));
        log.debug(SERVICE + "피드 삭제 완료: feedId = {}, userId = {}", feedId, userId);

        return feed;
    }

    private void validateCursorRequest(String cursor, String sortBy) {

        if (sortBy == null || (!sortBy.equals(SORT_BY_CREATED_AT) && !sortBy.equals(SORT_BY_LIKE_COUNT))) {
            throw new OtbooException(ErrorCode.INVALID_SORT_FIELD, Map.of("sortBy", sortBy));
        }

        if (cursor != null) {
            switch (sortBy) {
                case SORT_BY_CREATED_AT -> parseInstant(cursor);
                case SORT_BY_LIKE_COUNT -> parseLong(cursor);
            }
        }
    }

    private FeedDto convertToDto(Feed feed, UUID currentUserId) {

        boolean likedByMe = false;
        if (currentUserId != null) {
            likedByMe = feedLikeRepository.existsByFeedIdAndUserId(feed.getId(), currentUserId);
        }
        FeedDto feedDto = feedMapper.toDto(feed);

        if (feedDto == null) {
            throw new OtbooException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return feedDto.toBuilder().likedByMe(likedByMe).build();
    }

    private Instant parseInstant(String cursorValue) {
        try {
            return Instant.parse(cursorValue);
        } catch (DateTimeParseException e) {
            throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT, Map.of("cursor", cursorValue));
        }
    }

    private Long parseLong(String cursorValue) {
        try {
            return Long.parseLong(cursorValue);
        } catch (NumberFormatException e) {
            throw new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT, Map.of("cursor", cursorValue));
        }
    }
}
