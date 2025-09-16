package com.samsamotot.otboo.location.service;

import com.samsamotot.otboo.location.entity.WeatherAPILocation;

public interface LocationService {

    WeatherAPILocation getCurrentLocation(double longitude, double latitude);
}
