package com.samsamotot.otboo.weather.dto;

import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.WindAsWord;
import lombok.Builder;

/**
 * PackageName  : com.samsamotot.otboo.weather.dto
 * FileName     : ProfileServiceImpl
 * Author       : HuInDoL
 */
@Builder
public record WeatherChangeDto(
        // 변화된 날씨 정보만 담기 위한 DTO. 모든 필드는 null일 수 있습니다.
        Double tempComparedToDayBefore,
        Double humidComparedToDayBefore,
        SkyStatus skyStatus,
        Precipitation precipitation,
        WindAsWord windAsWord
) {
    public boolean hasChanges() {
        return tempComparedToDayBefore != null ||
                humidComparedToDayBefore != null ||
                skyStatus != null ||
                precipitation != null ||
                windAsWord != null;
    }
}
