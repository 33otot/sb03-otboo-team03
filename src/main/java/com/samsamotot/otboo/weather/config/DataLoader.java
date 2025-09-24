package com.samsamotot.otboo.weather.config;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private static final String CONFIG_NAME = "[DataLoader] ";

    private static final String CSV_FILE_PATH = "locations_korea.csv";

    private final LocationRepository locationRepository;


    @Override
    public void run(ApplicationArguments args) {
        if (locationRepository.count() > 3800) {
            log.info(CONFIG_NAME + "대한민국 위치 정보가 이미 존재합니다.");
            return;
        }

        log.info(CONFIG_NAME + "Location 데이터베이스 초기화 시작");
        locationRepository.deleteAll();

        ClassPathResource resource = new ClassPathResource(CSV_FILE_PATH);
        List<Location> locations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // 헤더 라인 건너뛰기

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 7) {
                    Location location = null;
                    try {
                        location = Location.builder()
                                .x(Integer.parseInt(parts[0].trim()))
                                .y(Integer.parseInt(parts[1].trim()))
                                .locationNames(buildLocationNames(parts))
                                .longitude(Double.parseDouble(parts[parts.length - 2].trim()))
                                .latitude(Double.parseDouble(parts[parts.length - 1].trim()))
                                .build();
                    } catch (NumberFormatException e) {
                        log.warn(CONFIG_NAME + "CSV 파일 파싱 중 숫자 변환 오류. 라인 건너뜀: {}", line);
                    }

                    locations.add(location);
                }
            }
        } catch (Exception e) {
            log.error(CONFIG_NAME + "CSV 파일({})을 읽는 중 심각한 오류가 발생하여 초기화를 중단합니다.", CSV_FILE_PATH, e);
            // 초기화 실패 시, 애플리케이션이 시작되지 않도록 런타임 예외를 다시 던질 수 있습니다.
            throw new RuntimeException("DataLoader 실행 실패: CSV 파일 처리 중 오류 발생", e);
        }
        try {
            locationRepository.saveAll(locations);
            log.info(CONFIG_NAME + "{}개의 Location 데이터 초기화 완료.", locations.size());
        } catch (Exception e) {
            log.error(CONFIG_NAME + "Location 데이터를 DB에 저장하는 중 심각한 오류가 발생했습니다.", e);
            // DB 저장 실패 시에도 원인을 파악하기 위해 예외를 다시 던짐
            throw new RuntimeException("DataLoader 실행 실패: DB 저장 중 오류 발생", e);
        }
    }

    /**
     * CSV 라인의 지역 파트(1단계, 2단계, 3단계 등)를 List<String>으로 변환합니다.
     * 위도, 경도를 제외한 중간 부분을 지역명으로 간주합니다.
     */
    private List<String> buildLocationNames(String[] parts) {
        return Arrays.stream(parts, 2, parts.length - 2)
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
