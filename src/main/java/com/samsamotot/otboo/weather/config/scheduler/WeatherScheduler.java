package com.samsamotot.otboo.weather.config.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private static final String SCHEDULER_NAME = "[WeatherScheduler] ";

    private final JobLauncher jobLauncher;
    private final Job weatherJob;

    // 3시간 주기로 매시 10분에 실행 (2:10, 5:10, 8:10, 11:10, 14:10, 17:10, 20:10, 23:10)
    @Scheduled(cron = "${schedules.cron.weather-batch}")
    @SchedulerLock(
            name = "weatherJob", // 락 고유 이름
            lockAtMostFor = "PT30M",  // 최대 30분간 락 유지
            lockAtLeastFor = "PT10M"   // 최소 10분간 락 유지
    )
    public void runWeatherJob() {
        log.info(SCHEDULER_NAME + "날씨 배치 작업 스케줄러 실행, 락 획득 시도...");
        try {
            // 매번 다른 Job ID를 부여하여 Job이 재실행되도록 함
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(weatherJob, params);
            log.info(SCHEDULER_NAME + "날씨 배치 작업 실행 완료.");
        } catch (JobExecutionAlreadyRunningException e) {
            // ShedLock을 사용하면 이 예외는 거의 발생하지 않지만, 안전장치로 유지
            log.warn("배치 작업이 이미 실행 중입니다. (다른 인스턴스일 수 있음)", e);
        } catch (Exception e) {
            log.error(SCHEDULER_NAME + "날씨 정보 배치 작업 중 오류 발생", e);
        }
        // 락을 획득하지 못한 인스턴스는 이 메서드 자체를 실행하지 않고 조용히 종료됨
    }
}
