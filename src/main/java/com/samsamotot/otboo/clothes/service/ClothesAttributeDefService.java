package com.samsamotot.otboo.clothes.service;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import java.util.List;
import java.util.UUID;

/**
 * 의상 속성 정의의 비즈니스 로직을 담당하는 인터페이스
 */
public interface ClothesAttributeDefService {
    ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);

    ClothesAttributeDefDto update(UUID defId, ClothesAttributeDefUpdateRequest request);

    void delete(UUID defId);

    List<ClothesAttributeDefDto> findAll(String sortBy, String sortDirection, String keywordLike);
}
