package com.samsamotot.otboo.weather.config.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private static final String SCHEDULER_NAME = "[WeatherScheduler] ";

    private final JobLauncher jobLauncher;
    private final Job weatherJob;

//    @Scheduled(cron = "0 */5 * * * *") // 테스트용 5분마다 배치 수집
    // 3시간 주기로 매시 10분에 실행 (2:10, 5:10, 8:10, 11:10, 14:10, 17:10, 20:10, 23:10)
    @Scheduled(cron = "0 10 2,5,8,11,14,17,20,23 * * *")
    public void runWeatherJob() {
        try {
            // 매번 다른 Job ID를 부여하여 Job이 재실행됨
            JobParameters params = new JobParametersBuilder()
                    .addString("jobId", String.valueOf(System.currentTimeMillis()))
                    .toJobParameters();
            jobLauncher.run(weatherJob, params);
        } catch (Exception e) {
            log.error(SCHEDULER_NAME + "날씨 정보 배치 작업 중 오류 발생", e);
        }
    }
}
