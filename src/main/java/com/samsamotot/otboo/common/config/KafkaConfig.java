package com.samsamotot.otboo.common.config;

import static org.springframework.kafka.listener.ContainerProperties.AckMode.BATCH;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * PackageName  : com.samsamotot.otboo.common.config
 * FileName     : KafkaConfig
 * Author       : dounguk
 * Date         : 2025. 10. 17.
 * Description  : Kafka 설정 및 Producer/Consumer 구성
 */
@Configuration
@EnableKafka
//@RequiredArgsConstructor
@Slf4j
public class KafkaConfig {

    private static final String KAFKA_CONFIG = "[KafkaConfig] ";

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
        log.info(KAFKA_CONFIG + "KafkaConfig 초기화 - KafkaProperties 주입 완료");
    }

    /**
     * [String용] Kafka Producer 설정
     */
    @Bean
    // 기본 ProducerFactory는 String을 처리하도록 유지
    public ProducerFactory<String, String> producerFactory() {
        if (kafkaProperties.getBootstrapServers() == null || kafkaProperties.getBootstrapServers().isEmpty()) {
            log.warn(KAFKA_CONFIG + "Kafka 비활성화 - bootstrapServers가 설정되지 않음");
            return null;
        }

        String bootstrapServers = String.join(",", kafkaProperties.getBootstrapServers());
        log.info(KAFKA_CONFIG + "Producer Factory 설정 시작 - bootstrapServers: {}", bootstrapServers);

        // Spring Boot가 application.yaml의 모든 Kafka 설정을 자동으로 읽어옴
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);

        // SASL 설정 확인 로그
        if (configProps.containsKey("security.protocol")) {
            log.info(KAFKA_CONFIG + "SASL 인증 설정 확인 - protocol: {}, mechanism: {}",
                configProps.get("security.protocol"),
                configProps.get("sasl.mechanism"));
        }

        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 배치 크기 : 16KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5); // 배치 대기 시간
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 버퍼 메모리 크기 : 32MB
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,
            "org.apache.kafka.clients.producer.internals.DefaultPartitioner");

        log.info(KAFKA_CONFIG + "Producer 설정 완료 - 배치크기: 16KB, 버퍼메모리: 32MB, 대기시간: 5ms");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * [String용] Kafka Template Bean
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        ProducerFactory<String, String> factory = producerFactory();
        if (factory == null) {
            log.warn(KAFKA_CONFIG + "KafkaTemplate 비활성화 - ProducerFactory가 null");
            return null;
        }

        log.info(KAFKA_CONFIG + "KafkaTemplate Bean 생성");
        return new KafkaTemplate<>(factory);
    }

    /**
     * [JSON용] Kafka Producer 설정
     */
    @Bean
    public ProducerFactory<String, Object> jsonProducerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // Value는 JsonSerializer 사용
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true); // 타입 정보 헤더 추가

        log.info(KAFKA_CONFIG + "JSON Producer Factory 설정 완료");

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * [JSON용] Kafka Template Bean
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaJsonTemplate() {
        ProducerFactory<String, Object> factory = jsonProducerFactory();
        if (factory == null) {
            log.warn(KAFKA_CONFIG + "JSON KafkaTemplate 비활성화 - jsonProducerFactory가 null");
            return null;
        }
        log.info(KAFKA_CONFIG + "JSON KafkaTemplate Bean 생성");
        return new KafkaTemplate<>(factory);
    }


    /**
     * [String용] Kafka Consumer 설정
     */
    @Bean
    // 기본 ConsumerFactory는 String을 처리하도록 유지
    public ConsumerFactory<String, String> consumerFactory() {
        if (kafkaProperties.getBootstrapServers() == null || kafkaProperties.getBootstrapServers().isEmpty()) {
            log.warn(KAFKA_CONFIG + "Kafka Consumer 비활성화 - bootstrapServers가 설정되지 않음");
            return null;
        }
        String bootstrapServers = String.join(",", kafkaProperties.getBootstrapServers());
        String groupId = kafkaProperties.getConsumer().getGroupId();
        log.info(KAFKA_CONFIG + "Consumer Factory 설정 시작 - bootstrapServers: {}, groupId: {}", bootstrapServers, groupId);

        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties(null);

        // SASL 설정 확인 로그
        if (configProps.containsKey("security.protocol")) {
            log.info(KAFKA_CONFIG + "SASL 인증 설정 확인 - protocol: {}, mechanism: {}",
                configProps.get("security.protocol"),
                configProps.get("sasl.mechanism"));
        }

        // 오프셋 관리 설정
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋 사용
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000); // 커밋 간격 (수동 커밋에서는 무시됨)

        // 성능 설정
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // 한 번에 가져올 최대 레코드 수
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1); // 최소 fetch 크기
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // 최대 fetch 대기 시간

        log.info(KAFKA_CONFIG + "Consumer 설정 완료 - 오프셋: latest, 수동커밋: BATCH, 최대폴링: 500개");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * [String용] Kafka Listener Container Factory 설정
     */
    @Bean
    // 기본 ListenerFactory는 String을 처리하도록 유지
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConsumerFactory<String, String> consumerFactory = consumerFactory();
        if (consumerFactory == null) {
            log.warn(KAFKA_CONFIG + "Kafka Listener Container Factory 비활성화 - ConsumerFactory가 null");
            return null;
        }

        log.info(KAFKA_CONFIG + "Kafka Listener Container Factory 설정 시작");

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties().setAckMode(BATCH);
        factory.getContainerProperties().setPollTimeout(3000);

        factory.setCommonErrorHandler(new DefaultErrorHandler((record, exception) -> {
            log.error(KAFKA_CONFIG + "Kafka 메시지 처리 중 오류 발생: {}", exception.getMessage(), exception);
        }));

        log.info(KAFKA_CONFIG + "Listener Container Factory 설정 완료 - AckMode: BATCH, PollTimeout: 3s");

        return factory;
    }

    /**
     * [JSON용] Kafka Consumer 설정
     */
    @Bean
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.remove(ConsumerConfig.GROUP_ID_CONFIG);

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class); // Value는 JsonDeserializer 사용
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.samsamotot.otboo.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        log.info(KAFKA_CONFIG + "JSON Consumer Factory 설정 완료");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(Object.class));
    }

    /**
     * [JSON용] Kafka Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaJsonListenerContainerFactory() {
        ConsumerFactory<String, Object> consumerFactory = jsonConsumerFactory();
        if (consumerFactory == null) {
            log.warn(KAFKA_CONFIG + "JSON Kafka Listener Container Factory 비활성화 - jsonConsumerFactory가 null");
            return null;
        }

        log.info(KAFKA_CONFIG + "JSON Kafka Listener Container Factory 설정 시작");

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties().setAckMode(BATCH);
        factory.setCommonErrorHandler(new DefaultErrorHandler((record, exception) -> {
            log.error(KAFKA_CONFIG + "JSON Kafka 메시지 처리 중 오류 발생: {}", exception.getMessage(), exception);
        }));

        return factory;
    }
}
