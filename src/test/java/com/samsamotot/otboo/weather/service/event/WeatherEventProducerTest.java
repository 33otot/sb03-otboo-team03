package com.samsamotot.otboo.weather.service.event;

import com.samsamotot.otboo.weather.dto.event.WeatherUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherEventProducer 단위 테스트")
public class WeatherEventProducerTest {

    private WeatherEventProducer weatherEventProducer;

    @Mock
    private KafkaTemplate<String, Object> kafkaJsonTemplate;

    @BeforeEach
    void setUp() {
        weatherEventProducer = new WeatherEventProducer(kafkaJsonTemplate);
    }

    @Test
    void 날씨_배치_이벤트를_Kafka_토픽으로_정상_발행한다() {
        // Given
        ArgumentCaptor<WeatherUpdateEvent> eventCaptor = ArgumentCaptor.forClass(WeatherUpdateEvent.class);

        // When
        weatherEventProducer.sendWeatherUpdateEvent(60, 127);

        // Then
        verify(kafkaJsonTemplate).send(ArgumentCaptor.forClass(String.class).capture(), eventCaptor.capture());
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaJsonTemplate).send(topicCaptor.capture(), eventCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("weather-update-topic");
        assertThat(eventCaptor.getValue().x()).isEqualTo(60);
        assertThat(eventCaptor.getValue().y()).isEqualTo(127);
    }
}
