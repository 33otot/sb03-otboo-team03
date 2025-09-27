package com.samsamotot.otboo.weather.dto;


import java.util.List;

public record WeatherForecastResponse(Response response) {
    public record Response(Header header, Body body) {}
    public record Header(String resultCode, String resultMsg) {}
    public record Body(
            String dataType,
            Items items,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {}
    public record Items(List<Item> item) {}
    public record Item(
            String baseDate,
            String baseTime,
            String category,
            String fcstDate,
            String fcstTime,
            String fcstValue,
            String nx,
            String ny
    ) {}
}

