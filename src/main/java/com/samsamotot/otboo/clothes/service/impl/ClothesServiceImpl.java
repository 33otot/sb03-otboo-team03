package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeWithDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.service.ClothesService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 관련 비즈니스 로직을 담당하는 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

    @Override
    public ClothesDto create(ClothesCreateRequest request,
        Optional<MultipartFile> optionalClothesImage) {

        // imageUrl 처리 - 이미지가 있으면 더미 URL, 없으면 null
        String imageUrl = optionalClothesImage.isPresent()
            ? "https://dummy-s3-url.com/test.jpg"
            : null;

        List<ClothesAttributeWithDefDto> attributes;

        if (request.items() == null) {
            attributes = Collections.emptyList();
        } else {
            attributes = request.items().stream()
                .map(attr -> new ClothesAttributeWithDefDto(
                    attr.definitionId(),
                    null,
                    Collections.emptyList(),
                    attr.value()
                ))
                .toList();
        }

        // DTO 반환
        return new ClothesDto(
            // 임시 id
            UUID.randomUUID(),
            request.ownerId(),
            request.name(),
            imageUrl,
            request.type(),
            attributes
        );
    }
}
