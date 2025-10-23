package com.samsamotot.otboo.weather.dto;

import com.samsamotot.otboo.weather.entity.WindAsWord;
import lombok.Builder;

@Builder
public record WindSpeedDto(
    Double speed,
    WindAsWord asWord
) {

}

