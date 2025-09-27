package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.weather.entity.Grid;

public class GridFixture {

    public static final int DEFAULT_X = 60;
    public static final int DEFAULT_Y = 127;

    public static Grid createGrid() {
        return Grid.builder()
                .x(DEFAULT_X)
                .y(DEFAULT_Y)
                .build();
    }
}
