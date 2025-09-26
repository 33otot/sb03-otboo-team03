package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import com.samsamotot.otboo.clothes.service.ClothesService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            log.debug("[ClothesController] 이미지 있는 의상 등록 요청 - clothesService.create 호출");
            result = clothesService.create(request, image);
            log.debug("[ClothesController] 이미지 있는 의상 등록 요청 - 결과 반환: id: {}", result.id());
        }
        else {
            log.debug("[ClothesController] 이미지 없는 의상 등록 요청 - clothesService.create 호출");
            result = clothesService.create(request);
            log.debug("[ClothesController] 이미지 없는 의상 등록 요청 - 결과 반환: id: {}", result.id());
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }

    @PatchMapping("/{clothesId}")
    public ResponseEntity<ClothesDto> updateClothes(
        @PathVariable("clothesId") UUID clothesId,
        @Valid @RequestPart("clothesUpdateRequest") ClothesUpdateRequest request,
        @RequestPart(value = "clothesImage", required = false) MultipartFile image
    ) {
        ClothesDto result;
        if (image != null) {
            log.debug("[ClothesController] 이미지 있는 의상 수정 요청 - clothesService.update 호출");
            result = clothesService.update(clothesId, request, image);
            log.debug("[ClothesController] 이미지 있는 의상 수정 요청 - 결과 반환: id: {}", result.id());
        }
        else {
            log.debug("[ClothesController] 이미지 없는 의상 수정 요청 - clothesService.update 호출");
            result = clothesService.update(clothesId, request);
            log.debug("[ClothesController] 이미지 없는 의상 수정 요청 - 결과 반환: id: {}", result.id());
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }
}