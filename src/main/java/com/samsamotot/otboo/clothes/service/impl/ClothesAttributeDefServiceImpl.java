package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.mapper.ClothesAttributeDefMapper;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.clothes.definition.ClothesAttributeDefAlreadyExist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의상 속성 정의의 비즈니스 로직을 담당하는 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository defRepository;
    private final ClothesAttributeDefMapper defMapper;

//    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {

        // 이미 존재하는 정의인지 확인
        if (defRepository.existsByName(request.name())) {
            throw new ClothesAttributeDefAlreadyExist(ErrorCode.CLOTHES_ATTRIBUTE_DEF_ALREADY_EXISTS);
        }

        // 엔티티 생성
        ClothesAttributeDef def = ClothesAttributeDef.createClothesAttributeDef(request.name());

        // 옵션이랑 연관관계
        request.selectableValues().forEach(value -> {
            ClothesAttributeOption option = ClothesAttributeOption.createClothesAttributeOption(def, value);
            def.addOption(option);
        });

        ClothesAttributeDef saved = defRepository.save(def);

        return defMapper.toDto(saved);
    }
}
