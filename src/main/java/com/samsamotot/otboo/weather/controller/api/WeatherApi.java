package com.samsamotot.otboo.weather.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.location.entity.WeatherAPILocation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "날씨 관리", description = "날씨 관련 API")
public interface WeatherApi {

    @Operation(summary = "현재 위치 정보 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "위치 정보 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = WeatherAPILocation.class)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "위치 정보 조회 실패",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation =  ErrorResponse.class)
                )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<WeatherAPILocation> getCurrentLocation(
        @Parameter(description = "경도 (WGS84 좌표계)")
        @RequestParam double longitude,
        
        @Parameter(description = "위도 (WGS84 좌표계)")
        @RequestParam double latitude
    );
}
