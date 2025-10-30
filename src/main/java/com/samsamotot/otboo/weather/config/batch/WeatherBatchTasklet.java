package com.samsamotot.otboo.weather.config.batch;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.service.WeatherService;
import com.samsamotot.otboo.weather.service.event.WeatherEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBatchTasklet implements Tasklet {

    private static final String TASKLET_NAME = "[WeatherBatchTasklet] ";

    private final LocationRepository locationRepository;
    private final WeatherEventProducer weatherEventProducer;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info(TASKLET_NAME + "모든 활성 격자에 대한 날씨 업데이트 이벤트 발행 시작");

        // 1. 보유한 위치 정보가 있는 격자만 조회
        List<Location> activeLocations = locationRepository.findAllWithGrid();

        // 2. 고유한 Grid의 좌표(x, y)를 추출하여 Kafka 이벤트 발행
        activeLocations.stream()
                .map(Location::getGrid)
                .filter(Objects::nonNull)
                .distinct() // 중복된 Grid 제거
                .forEach(grid -> weatherEventProducer.sendWeatherUpdateEvent(grid.getX(), grid.getY()));
        log.info(TASKLET_NAME + "모든 활성 격자에 대한 날씨 업데이트 이벤트 발행 완료");

        return RepeatStatus.FINISHED;
    }
}
