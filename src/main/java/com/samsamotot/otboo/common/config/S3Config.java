package com.samsamotot.otboo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3Client를 Bean으로 등록하는 설정 클래스
 */
@Configuration
@ConditionalOnProperty(
    name = "otboo.storage.type",
    havingValue = "s3"
)
public class S3Config {
    @Value("${otboo.storage.s3.access-key}")
    private String accessKey;

    @Value("${otboo.storage.s3.secret-key}")
    private String secretKey;

    @Value("${otboo.storage.s3.region}")
    private String region;

    // S3 빈 설정
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}
