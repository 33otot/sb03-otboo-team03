package com.samsamotot.otboo.weather.service.event;

import com.samsamotot.otboo.weather.dto.event.WeatherUpdateEvent;
import com.samsamotot.otboo.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherEventConsumer {

    private static final String SERVICE_NAME = "[WeatherEventConsumer] ";

    private static final String TOPIC = "weather-update-topic";
    private final WeatherService weatherService;

    /**
     * 'weather-update-topic' 토픽을 구독하여 날씨 업데이트를 비동기적으로 처리
     *
     * @param event Kafka로부터 수신할 위치 정보 이벤트
     */
    @KafkaListener(
            topics = TOPIC,
            groupId = "otboo-weather-group",
            containerFactory = "kafkaJsonListenerContainerFactory" // JSON 전용 리스너 팩토리 지정
    )
    public void consumeWeatherUpdateEvent(WeatherUpdateEvent event) {
        log.info(SERVICE_NAME + "날씨 업데이트 이벤트 수신. Event: {}", event);
        // 이벤트에 담긴 x, y 좌표로 실제 날씨 업데이트 로직 호출
        weatherService.updateWeatherForGrid(event.x(), event.y());
        log.info(SERVICE_NAME + "날씨 업데이트 처리 완료. Event: {}", event);
    }
}
