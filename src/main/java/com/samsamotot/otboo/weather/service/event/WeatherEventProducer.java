package com.samsamotot.otboo.weather.service.event;

import com.samsamotot.otboo.weather.dto.event.WeatherUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WeatherEventProducer {

    private static final String SERVICE_NAME = "[WeatherEventProducer] ";

    private static final String TOPIC = "weather-update-topic";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WeatherEventProducer(
            @Qualifier("kafkaJsonTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendWeatherUpdateEvent(int x, int y) {
        WeatherUpdateEvent event = new WeatherUpdateEvent(x, y);
        kafkaTemplate.send(TOPIC, event);
        log.info(SERVICE_NAME + "날씨 업데이트 이벤트 발행 완료. Topic: {}, Event: {}", TOPIC, event);
    }
}
