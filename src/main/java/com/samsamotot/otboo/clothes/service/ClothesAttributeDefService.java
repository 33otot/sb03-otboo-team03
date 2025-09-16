package com.samsamotot.otboo.clothes.service;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;

/**
 * 의상 속성 정의의 비즈니스 로직을 담당하는 인터페이스
 */
public interface ClothesAttributeDefService {
    ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);
}
