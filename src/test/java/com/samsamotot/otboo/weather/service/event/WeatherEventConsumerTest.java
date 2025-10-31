package com.samsamotot.otboo.weather.service.event;

import com.samsamotot.otboo.weather.dto.event.WeatherUpdateEvent;
import com.samsamotot.otboo.weather.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherEventConsumer 단위 테스트")
public class WeatherEventConsumerTest {

    @InjectMocks
    private WeatherEventConsumer weatherEventConsumer;

    @Mock
    private WeatherService weatherService;

    @Test
    void 날씨_배치_이벤트를_수신하면_WeatherService_호출하여_날씨_배치작업_수행() {
        // Given
        WeatherUpdateEvent event = new WeatherUpdateEvent(60, 127);

        // When
        weatherEventConsumer.consumeWeatherUpdateEvent(event);

        // Then
        verify(weatherService, times(1)).updateWeatherForGrid(60, 127);
    }
}
