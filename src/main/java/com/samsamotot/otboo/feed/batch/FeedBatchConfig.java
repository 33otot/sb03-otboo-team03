package com.samsamotot.otboo.feed.batch;

import com.samsamotot.otboo.feed.repository.FeedRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
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
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FeedBatchConfig {

    public static final String JOB_NAME = "deleteSoftDeletedFeedsJob";
    public static final String STEP_NAME = "deleteSoftDeletedFeedsStep";
    public static final String READER_NAME = "softDeletedFeedReader";
    public static final String CONFIG_NAME = "[FeedBatchConfig] ";

    private final DataSource dataSource;
    private final FeedRepository feedRepository;

    @Value("${batch.feed.chunk-size:100}")
    private int chunkSize;

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
            .<UUID, UUID>chunk(chunkSize, transactionManager)
            .reader(softDeletedFeedReader(null))
            .writer(softDeletedFeedWriter())
            .build();
    }

    // ItemReader 정의
    @Bean
    @StepScope
    public JdbcPagingItemReader<UUID> softDeletedFeedReader(
        @Value("#{jobParameters['deadline']}") String deadlineStr
    ) {
        Instant deadline = Instant.parse(deadlineStr);

        PostgresPagingQueryProvider qp = new PostgresPagingQueryProvider();
        qp.setSelectClause("f.id");
        qp.setFromClause("from feeds f");
        qp.setWhereClause("where f.is_deleted = true and f.updated_at < :deadline");
        qp.setSortKeys(Map.of("id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<UUID>()
            .name(READER_NAME)
            .dataSource(dataSource)
            .queryProvider(qp)
            .parameterValues(Map.of("deadline", Timestamp.from(deadline)))
            .pageSize(chunkSize)
            .fetchSize(chunkSize)
            .rowMapper((rs, i) -> rs.getObject("id", UUID.class))
            .saveState(true)
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
