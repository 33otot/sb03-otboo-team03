package com.samsamotot.otboo.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenWeatherMapResponse(
        @JsonProperty("coord") Coord coord,
        @JsonProperty("weather") List<Weather> weather,
        @JsonProperty("main") Main main,
        @JsonProperty("wind") Wind wind,
        @JsonProperty("rain") Rain rain,
        @JsonProperty("snow") Snow snow,
        @JsonProperty("dt") long dt,
        @JsonProperty("name") String name
        ) {

    public record Coord(
            double lon,
            double lat
    ) {}

    public record Weather (
        int id,
        String main,
        String description,
        String icon
    ) {}

    public record Main(
            double temp,
            @JsonProperty("feels_like") double feelsLike,
            @JsonProperty("temp_min") double tempMin,
            @JsonProperty("temp_max") double tempMax,
            int pressure,
            int humidity
    ) {}

    public record Wind(double speed, int deg) {}

    public record Rain(@JsonProperty("1h") double oneHour) {}

    public record Snow(@JsonProperty("1h") double oneHour) {}
}
