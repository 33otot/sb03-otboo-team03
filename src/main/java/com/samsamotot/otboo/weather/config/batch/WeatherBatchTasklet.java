package com.samsamotot.otboo.weather.config.batch;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBatchTasklet implements Tasklet {

    private static final String TASKLET_NAME = "[WeatherBatchTasklet] ";

    private final LocationRepository locationRepository;
    private final WeatherService weatherService;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info(TASKLET_NAME + "전국 날씨 데이터 병렬 업데이트 배치 시작");

        // 1. DB에서 날씨를 업데이트할 모든 Location 목록 호출
        List<Location> allLocations = locationRepository.findAll();
        log.info(TASKLET_NAME + "{}개의 모든 지역에 대한 병렬처리 시작.", allLocations.size());

        // 2. 각 Location에 대해 비동기 작업 생성하여 스레드풀에 제출
        List<CompletableFuture<Void>> futures = allLocations.stream()
                .map(weatherService::updateWeatherDataForCoordinate)
                .toList();

        // 3. 모든 비동기 작업 완료까지 대기
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error(TASKLET_NAME + "모든 비동기 작업 완료 대기 중 에러 발생", e);
        }

        log.info(TASKLET_NAME + "전국 날씨 데이터 병렬 업데이트 배치 완료.");

        return RepeatStatus.FINISHED;
    }
}
