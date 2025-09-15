package com.samsamotot.otboo.clothes.service;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.springframework.stereotype.Service;

@Service
public interface ClothesAttributeDefService {
    ClothesAttributeDefDto createClothesAttributeDef(ClothesAttributeDefCreateRequest request);
}
