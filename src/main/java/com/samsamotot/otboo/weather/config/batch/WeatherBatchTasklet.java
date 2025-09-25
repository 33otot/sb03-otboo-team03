package com.samsamotot.otboo.weather.config.batch;

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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBatchTasklet implements Tasklet {

    private static final String TASKLET_NAME = "[WeatherBatchTasklet] ";

    private final WeatherService weatherService;
    private final GridRepository gridRepository;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info(TASKLET_NAME + "전국 격자별 날씨 데이터 병렬 업데이트 배치 시작");

        // 1. DB에서 날씨를 업데이트할 모든 Grid 목록 호출
        List<Grid> allGrids = gridRepository.findAll();
        log.info(TASKLET_NAME + "{}개의 고유 격자에 대한 병렬처리 시작.", allGrids.size());

        // 2. 각 Grid에 대해 비동기 날씨 업데이트 작업 생성
        List<CompletableFuture<Void>> futures = allGrids.stream()
                .map(weatherService::updateWeatherDataForGrid)
                .toList();

        // 3. 모든 비동기 작업 완료까지 대기
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            // 비동기 작업 중 하나라도 실패하면, 전체 Step을 실패 처리하기 위해 예외를 다시 던짐
            log.error(TASKLET_NAME + "하나 이상의 비동기 날씨 업데이트 작업 실패. Step을 실패 처리합니다.", e);
            throw new RuntimeException("비동기 날씨 업데이트 작업 중 오류 발생", e);
        }

        log.info(TASKLET_NAME + "전국 격자별 날씨 데이터 병렬 업데이트 배치 완료.");

        return RepeatStatus.FINISHED;
    }
}
