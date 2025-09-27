package com.samsamotot.otboo.weather.config;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private static final String CONFIG_NAME = "[DataLoader] ";

    private static final String CSV_FILE_PATH = "locations_korea.csv";
    private static final int GRID_COUNT = 1600;

    private final LocationRepository locationRepository;
    private final GridRepository gridRepository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (gridRepository.count() > GRID_COUNT) {
            log.info(CONFIG_NAME + "대한민국 격자 정보가 이미 존재합니다.");
            return;
        }

        log.info(CONFIG_NAME + "Location 및 Grid 데이터베이스 초기화 시작");
        
        // 기존 데이터 조회
        Map<String, Grid> existingGrids = gridRepository.findAll().stream()
                .collect(Collectors.toMap(grid -> grid.getX() + "_" + grid.getY(), grid -> grid));
        
        // 기존 Location 데이터 조회 (중복 체크용)
        Set<String> existingLocationKeys = locationRepository.findAll().stream()
                .map(loc -> loc.getLongitude() + "_" + loc.getLatitude())
                .collect(Collectors.toSet());

        ClassPathResource resource = new ClassPathResource(CSV_FILE_PATH);
        if (!resource.exists()) {
            log.warn(CONFIG_NAME + "CSV 파일({})이 존재하지 않아 초기화를 건너뜁니다.", CSV_FILE_PATH);
            return;
        }
        Map<String, Grid> newGrids = new HashMap<>();
        List<Location> newLocations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // 헤더 라인 건너뛰기

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 7) { // 최소한 7개의 컬럼이 있어야 합니다.
                    log.warn(CONFIG_NAME + "CSV 라인 형식이 올바르지 않습니다. 건너뜀: {}", line);
                    continue; // 유효하지 않은 라인은 건너뜁니다.
                }
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    String gridKey = x + "_" + y;
                    double longitude = Double.parseDouble(parts[parts.length - 2].trim());
                    double latitude = Double.parseDouble(parts[parts.length - 1].trim());
                    String locationKey = longitude + "_" + latitude;

                    // 1. Grid 중복 체크 - 기존에 없으면 새로 생성
                    Grid grid = existingGrids.get(gridKey);
                    if (grid == null) {
                        grid = newGrids.computeIfAbsent(gridKey, k -> Grid.builder().x(x).y(y).build());
                    }

                    // 2. Location 중복 체크 - 기존에 없으면 새로 생성
                    if (!existingLocationKeys.contains(locationKey)) {
                        Location location = Location.builder()
                                .longitude(longitude)
                                .latitude(latitude)
                                .grid(grid)
                                .locationNames(buildLocationNames(parts))
                                .build();
                        newLocations.add(location);
                    }
                } catch (Exception e) {
                    log.warn(CONFIG_NAME + "CSV 파일 파싱 중 숫자 변환 오류. 라인 건너뜀: {}", line);
                }
            }
            
            // 3. 새로운 Grid들과 Location들만 DB에 저장
            if (!newGrids.isEmpty()) {
                gridRepository.saveAll(newGrids.values());
                log.info(CONFIG_NAME + "{}개의 새로운 Grid 데이터 저장 완료", newGrids.size());
            }
            
            if (!newLocations.isEmpty()) {
                locationRepository.saveAll(newLocations);
                log.info(CONFIG_NAME + "{}개의 새로운 Location 데이터 저장 완료", newLocations.size());
            }

            log.info(CONFIG_NAME + "데이터 초기화 완료 - 기존 Grid: {}, 새 Grid: {}, 새 Location: {}", 
                    existingGrids.size(), newGrids.size(), newLocations.size());

        } catch (Exception e) {
            log.error(CONFIG_NAME + "CSV 파일({})을 읽는 중 심각한 오류가 발생하여 초기화를 중단합니다.", CSV_FILE_PATH, e);
            throw new RuntimeException("DataLoader 실행 실패: CSV 파일 처리 중 오류 발생", e);
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
