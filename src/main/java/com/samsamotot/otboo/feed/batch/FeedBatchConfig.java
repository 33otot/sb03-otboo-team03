package com.samsamotot.otboo.feed.batch;

import com.samsamotot.otboo.feed.repository.FeedRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class FeedBatchConfig {

    public static final String JOB_NAME = "deleteSoftDeletedFeedsJob";
    public static final String STEP_NAME = "deleteSoftDeletedFeedsStep";
    public static final String READER_NAME = "softDeletedFeedReader";
    public static final String CONFIG_NAME = "[FeedBatchConfig] ";

    private final EntityManagerFactory entityManagerFactory;
    private final FeedRepository feedRepository;

    private static final int CHUNK_SIZE = 100;

    // Job 정의
    @Bean
    public Job deleteSoftDeletedFeedsJob(JobRepository jobRepository, Step deleteSoftDeletedFeedsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(deleteSoftDeletedFeedsStep)
            .listener(jobExecutionListener())
            .build();
    }

    // Step 정의
    @Bean
    public Step deleteSoftDeletedFeedsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<UUID, UUID>chunk(CHUNK_SIZE, transactionManager)
            .reader(softDeletedFeedReader(null))
            .writer(softDeletedFeedWriter())
            .build();
    }

    // ItemReader 정의
    @Bean
    @StepScope
    public JpaCursorItemReader<UUID> softDeletedFeedReader(
        @Value("#{jobParameters['deadline']}") String deadlineStr
    ) {
        Instant deadline = Instant.parse(deadlineStr);
        return new JpaCursorItemReaderBuilder<UUID>()
            .name(READER_NAME)
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT f.id FROM Feed f WHERE f.isDeleted = true AND f.updatedAt < :deadline")
            .parameterValues(Map.of("deadline", deadline))
            .saveState(false)
            .build();
    }

    // ItemWriter 정의
    @Bean
    public ItemWriter<UUID> softDeletedFeedWriter() {
        return chunk -> {
            List<UUID> ids = new ArrayList<>(chunk.getItems());
            if (ids.isEmpty()) return;

            log.debug("Deleting {} soft-deleted feeds.", chunk.getItems().size());
            feedRepository.deleteAllByIdInBatch(ids);
        };
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(@NonNull JobExecution jobExecution) {
                log.info(CONFIG_NAME + "Job {} starts", jobExecution.getJobInstance().getJobName());
            }

            @Override
            public void afterJob(@NonNull JobExecution jobExecution) {
                log.info(CONFIG_NAME + "Job {} finished with status: {}.", jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
            }
        };
    }
}
