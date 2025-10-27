package com.samsamotot.otboo.feed.batch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
public class FeedBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job deleteSoftDeletedFeedsJob;

    private static final String SCHEDULER_NAME = "[FeedBatchScheduler] ";

    @Scheduled(cron = "0 0 4 * * *")
    public void runDeleteSoftDeletedFeedsJob() {
        try {
            Instant deadline = Instant.now().minus(7, ChronoUnit.DAYS);
            log.info(SCHEDULER_NAME + "7일 이상된 논리 삭제 피드 영구 삭제 배치 작업 시작");

            JobParameters params = new JobParametersBuilder()
                    .addString("run.id", String.valueOf(System.currentTimeMillis()))
                    .addString("deadline", deadline.toString())
                    .toJobParameters();

            jobLauncher.run(deleteSoftDeletedFeedsJob, params);
            log.info(SCHEDULER_NAME + "배치 작업 성공적으로 실행 완료");
        } catch (Exception e) {
            log.error(SCHEDULER_NAME + "배치 작업 실행 중 오류 발생", e);
        }
    }
}