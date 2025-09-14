package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
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

        log.info("[FeedServiceImpl] 피드 등록 시작: authorId = {}, weatherId = {}, clothesIds = {}", authorId, weatherId, clothesIds);

        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND, Map.of("authorId", authorId.toString())));

        Weather weather = weatherRepository.findById(weatherId)
                .orElseThrow(() -> new OtbooException(ErrorCode.WEATHER_NOT_FOUND, Map.of("weatherId", weatherId.toString())));

        List<Clothes> clothesList = clothesRepository.findAllById(clothesIds);
        if (clothesList.isEmpty()) {
            throw new OtbooException(ErrorCode.CLOTHES_NOT_FOUND, Map.of("clothesIds", clothesIds.toString()));
        }

        Feed feed = Feed.builder()
            .author(author)
            .weather(weather)
            .content(content)
            .build();

        for (Clothes c : clothesList){
            FeedClothes feedClothes = FeedClothes.builder()
                .feed(feed)
                .clothes(c)
                .build();

            feed.getFeedClothes().add(feedClothes);
        }

        Feed saved = feedRepository.save(feed);
        FeedDto result = feedMapper.toDto(saved);

        log.info("[FeedServiceImpl] 피드 등록 완료: feedId = {}", saved.getId());

        return result;
    }
}
