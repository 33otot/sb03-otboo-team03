package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import java.time.Instant;
import java.util.UUID;

/**
 * 의상 속성 정의의 비즈니스 로직을 담당하는 클래스
 */
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    @Override
    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {
        // 최소한의 코드
        return ClothesAttributeDefDto.builder()
            .id(UUID.randomUUID())
            .name(request.name())
            .selectableValues(request.selectableValues())
            .createdAt(Instant.now())
            .build();
    }
}
