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
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedLikeRepository;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedMapper feedMapper;

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

        log.debug("[FeedServiceImpl] 피드 등록 시작: authorId = {}, weatherId = {}, clothesIds = {}", authorId, weatherId, clothesIds);

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

        log.debug("[FeedServiceImpl] 피드 등록 완료: feedId = {}", saved.getId());

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

        log.debug("[FeedServiceImpl] 피드 목록 조회 시작: request = {}, userId = {}", request, userId);

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

        List<Feed> feeds = feedRepository.findByCursor(
            cursor,
            idAfter,
            limit + 1,
            sortBy,
            sortDirection,
            keywordLike,
            skyStatusEqual,
            precipitationTypeEqual,
            authorIdEqual
        );

        boolean hasNext = feeds.size() > limit;
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext) {
            Feed lastFeed =  feeds.get(limit - 1);
            nextCursor = resolveNextCursor(lastFeed, sortBy);
            nextIdAfter = lastFeed.getId();
            feeds = feeds.subList(0, limit);
        }

        long totalCount = feedRepository.countByFilter(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);

        List<FeedDto> feedDtos = convertToDtos(feeds, userId);

        log.info("[FeedServiceImpl] 피드 목록 조회 완료 - 조회된 피드 수:{}, hasNext: {}", feedDtos.size(), hasNext);
        return new CursorResponse<>(
            feedDtos,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    private void validateCursorRequest(String cursor, String sortBy) {

        if (!sortBy.equals(SORT_BY_CREATED_AT) && !sortBy.equals(SORT_BY_LIKE_COUNT)) {
            throw new OtbooException(ErrorCode.INVALID_SORT_FIELD, Map.of("sortBy", sortBy));
        }

        if (cursor != null) {
            switch (sortBy) {
                case SORT_BY_CREATED_AT -> parseInstant(cursor);
                case SORT_BY_LIKE_COUNT -> parseLong(cursor);
            }
        }
    }

    private String resolveNextCursor(Feed feed, String sortBy) {
        return switch(sortBy) {
            case SORT_BY_CREATED_AT -> feed.getCreatedAt().toString();
            case SORT_BY_LIKE_COUNT -> String.valueOf(feed.getLikeCount());
            default -> throw new OtbooException(ErrorCode.INVALID_SORT_FIELD);
        };
    }

    private List<FeedDto> convertToDtos(List<Feed> feeds, UUID currentUserId) {

        if (feeds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> feedIds = feeds.stream()
            .map(Feed::getId)
            .toList();

        Set<UUID> likedFeedIds = (currentUserId == null)
            ? Collections.emptySet()
            : feedLikeRepository.findFeedLikeIdsByUserIdAndFeedIdIn(currentUserId, feedIds);

        return feeds.stream()
            .map(feed -> {
                FeedDto dto = feedMapper.toDto(feed);
                boolean likedByMe = (currentUserId != null) && likedFeedIds.contains(feed.getId());
                return dto.toBuilder().likedByMe(likedByMe).build();
            })
            .toList();
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
