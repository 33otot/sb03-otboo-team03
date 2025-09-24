package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDto;
import com.samsamotot.otboo.clothes.dto.ClothesAttributeWithDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.clothes.exception.definition.ClothesAttributeDefNotFoundException;
import com.samsamotot.otboo.clothes.repository.ClothesAttributeDefRepository;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.clothes.service.ClothesService;
import com.samsamotot.otboo.common.storage.S3ImageStorage;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.exception.UserNotFoundException;
import com.samsamotot.otboo.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 관련 비즈니스 로직을 담당하는 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefRepository defRepository;
    private final UserRepository userRepository;
    private final S3ImageStorage s3ImageStorage;


    // 이미지 없는 생성
    @Override
    @Transactional
    public ClothesDto create(ClothesCreateRequest request) {
        log.info("[ClothesServiceImpl] create - 이미지 없는 의상 생성 호출됨");

        List<ClothesAttributeWithDefDto> attributes = new ArrayList<>();

        // 유저 조회
        User owner = userRepository.findById(request.ownerId())
            .orElseThrow(() -> new UserNotFoundException());

        // Clothes 객체 생성
        Clothes clothes = Clothes.createClothes(
            request.name(),
            request.type(),
            owner
        );

        // Attributes 처리
        process(request.attributes(), clothes, attributes);

        Clothes saved = clothesRepository.save(clothes);

        log.info("[ClothesServiceImpl] ClothesDto 생성중");
        // DTO 반환
        return new ClothesDto(
            saved.getId(),
            request.ownerId(),
            request.name(),
            null,
            request.type(),
            attributes
        );
    }

    // 이미지 있는 생성
    @Override
    @Transactional
    public ClothesDto create(ClothesCreateRequest request,
        MultipartFile clothesImage) {
        log.info("[ClothesServiceImpl] create - 이미지 있는 의상 생성 호출됨");

        List<ClothesAttributeWithDefDto> attributes = new ArrayList<>();

        // 유저 조회
        User owner = userRepository.findById(request.ownerId())
            .orElseThrow(() -> new UserNotFoundException());

        // Clothes 객체 생성
        Clothes clothes = Clothes.createClothes(
            request.name(),
            request.type(),
            owner
        );

        // Attributes 처리
        process(request.attributes(), clothes, attributes);

        Clothes saved = clothesRepository.save(clothes);

        // imageUrl 처리
        String imageUrl;
        log.debug("[ClothesServiceImpl] S3에 썸네일 이미지 업로드 요청: {}", clothesImage.getOriginalFilename());
        imageUrl = s3ImageStorage.uploadImage(clothesImage, "clothes/");

        log.info("[ClothesServiceImpl] ClothesDto 생성중");
        // DTO 반환
        return new ClothesDto(
            saved.getId(),
            request.ownerId(),
            request.name(),
            imageUrl,
            request.type(),
            attributes
        );
    }

    /**
     *
     * @param attributes
     * @param clothes
     * @param clothesAttributeWithDefDtoList
     *
     * 1. 요청받은 dto에서 선택한 속성 값으로 def 객체를 조회하고 attribute 객체와 연관관계를 세팅함
     * 2. ClothesDto에 사용할 ClothesAttributeWithDefDto을 생성하고 List에 추가함
     */
    private void process(List<ClothesAttributeDto> attributes, Clothes clothes, List<ClothesAttributeWithDefDto> clothesAttributeWithDefDtoList) {
        // Attributes 처리
        if (attributes != null) {
            for (ClothesAttributeDto dto : attributes) {
                log.debug("[ClothesServiceImpl] Attribute 처리와 ClothesAttributeWithDefDto 생성을 위한 반복문");
                log.debug("[ClothesServiceImpl] {} 에서 현재 위치: {}", attributes, dto);
                // def 객체 조회
                ClothesAttributeDef definition = defRepository.findById(dto.definitionId())
                    .orElseThrow(() -> new ClothesAttributeDefNotFoundException());
                ClothesAttribute attribute = ClothesAttribute.createClothesAttribute(definition, dto.value());

                // 연관관계 세팅
                clothes.addAttribute(attribute);

                // ClothesAttributeWithDefDto 생성
                ClothesAttributeWithDefDto withDefDto = new ClothesAttributeWithDefDto(
                    dto.definitionId(),
                    definition.getName(),
                    definition.getOptions().stream()
                        .map(ClothesAttributeOption::getValue)
                        .toList(),
                    dto.value()
                );
                clothesAttributeWithDefDtoList.add(withDefDto);
            }
        }
        else {
            clothesAttributeWithDefDtoList = Collections.emptyList();
        }
    }
}
