package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.service.ClothesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 컨트롤러 메서드
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
@RestController
public class ClothesController implements ClothesControllerApi{

    private final ClothesService clothesService;

    @PostMapping
    public ResponseEntity<ClothesDto> createClothes(
        @Valid @RequestPart("clothesCreateRequest") ClothesCreateRequest request,
        @RequestPart(value = "clothesImage", required = false) MultipartFile image
    ) {
        ClothesDto result;
        if (image != null) {
            log.info("[ClothesController] 이미지 있는 의상 등록 요청 - clothesService.create 호출");
            result = clothesService.create(request, image);
            log.info("[ClothesController] 이미지 있는 의상 등록 요청 - clothesService.create 결과 반환: {}", result);
        }
        else {
            log.info("[ClothesController] 이미지 없는 의상 등록 요청 - clothesService.create 호출");
            result = clothesService.create(request);
            log.info("[ClothesController] 이미지 없는 의상 등록 요청 - clothesService.create 결과 반환: {}", result);
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }
}