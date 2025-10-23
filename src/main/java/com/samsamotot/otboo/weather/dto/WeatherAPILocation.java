package com.samsamotot.otboo.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "기상청 단기예보 API 위치 정보")
@Builder
public record WeatherAPILocation(
        @Schema(description = "위도")
        double latitude,
        
        @Schema(description = "경도")
        double longitude,
        
        @Schema(description = "카카오맵 X 좌표")
        Integer x,
        
        @Schema(description = "카카오맵 Y 좌표")
        Integer y,
        
        @Schema(description = "행정구역명 배열")
        List<String> locationNames
) {
}
