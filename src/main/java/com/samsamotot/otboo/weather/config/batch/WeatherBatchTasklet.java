package com.samsamotot.otboo.weather.config.batch;

import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import com.samsamotot.otboo.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBatchTasklet implements Tasklet {

    private static final String TASKLET_NAME = "[WeatherBatchTasklet] ";

    private final WeatherService weatherService;
    private final LocationRepository locationRepository;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info(TASKLET_NAME + "보유한 위치 정보가 있는 격자별 날씨 데이터 병렬 업데이트 배치 시작");

        // 1. 보유한 위치 정보가 있는 격자만 조회
        Set<Grid> gridsWithLocations = locationRepository.findAll().stream()
                .map(location -> location.getGrid())
                .collect(Collectors.toSet());

        List<Grid> targetGrids = List.copyOf(gridsWithLocations);
        log.info(TASKLET_NAME + "{}개의 위치 정보가 있는 격자에 대한 병렬처리 시작.", targetGrids.size());

        if (targetGrids.isEmpty()) {
            log.warn(TASKLET_NAME + "수집할 위치 정보가 없습니다. 배치를 종료합니다.");
            return RepeatStatus.FINISHED;
        }

        // 2. 각 Grid에 대해 비동기 날씨 업데이트 작업 생성
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> futures = targetGrids.stream()
                .map(grid -> weatherService.updateWeatherDataForGrid(grid.getId())
                        .whenComplete((result, error) -> { if (error != null) errors.add(error); }))
                .toList();

        // 3. 모든 비동기 작업 완료까지 대기
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            if (!errors.isEmpty()) {
                int successCount = targetGrids.size() - errors.size();
                int failureCount = errors.size();

                log.warn(TASKLET_NAME + "일부 격자 업데이트 실패 - 성공: {}건, 실패: {}건", successCount, failureCount);

                // 실패율이 너무 높은 경우에만 배치 실패 (예: 50% 이상)
                double failureRate = (double) failureCount / targetGrids.size();
                if (failureRate > 0.5) {
                    log.error(TASKLET_NAME + "실패율이 너무 높음 ({}%). 배치를 실패 처리합니다.", failureRate * 100);
                    throw new RuntimeException("비동기 날씨 업데이트 실패율 초과: " + failureRate * 100 + "%");
                }
            }
        } catch (Exception e) {
            // 비동기 작업 중 하나라도 실패하면, 전체 Step을 실패 처리하기 위해 예외를 다시 던짐
            log.error(TASKLET_NAME + "하나 이상의 비동기 날씨 업데이트 작업 실패. Step을 실패 처리합니다.", e);
            throw new RuntimeException("비동기 날씨 업데이트 작업 중 오류 발생", e);
        }

        log.info(TASKLET_NAME + "위치 정보가 있는 격자별 날씨 데이터 {}개 병렬 업데이트 배치 완료.", futures.size());

        return RepeatStatus.FINISHED;
    }
}
