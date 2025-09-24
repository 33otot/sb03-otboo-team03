package com.samsamotot.otboo.weather.config.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@EnableBatchProcessing
@Configuration
@RequiredArgsConstructor
public class WeatherBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final WeatherBatchTasklet weatherBatchTasklet;

    // Job 정의
    @Bean
    public Job weatherJob() {
        return new JobBuilder("weatherJob", jobRepository)
                .start(weatherTaskletStep())
                .build();
    }

    // Step 정의
    @Bean
    public Step weatherTaskletStep() {
        return new StepBuilder("weatherTaskletStep", jobRepository)
                .tasklet(weatherBatchTasklet, transactionManager)
                .build();
    }
}
