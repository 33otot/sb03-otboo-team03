package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import com.samsamotot.otboo.clothes.service.ClothesService;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.util.AuthUtil;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    private static final String CONTROLLER_NAME = "[ClothesController]";

    private final ClothesService clothesService;

    // 의상 등록 요청 컨트롤러
    @PostMapping
    public ResponseEntity<ClothesDto> createClothes(
        @Valid @RequestPart("request") ClothesCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UUID ownerId = AuthUtil.getAuthenticatedUserId();

        ClothesDto result;
        if (image != null) {
            log.debug(CONTROLLER_NAME + " 이미지 있는 의상 등록 요청 - clothesService.create 호출");
            result = clothesService.create(ownerId, request, image);
            log.debug(CONTROLLER_NAME + " 이미지 있는 의상 등록 요청 - 결과 반환: id: {}", result.id());
        }
        else {
            log.debug(CONTROLLER_NAME + " 이미지 없는 의상 등록 요청 - clothesService.create 호출");
            result = clothesService.create(ownerId, request);
            log.debug(CONTROLLER_NAME + " 이미지 없는 의상 등록 요청 - 결과 반환: id: {}", result.id());
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }

    // 의상 수정 요청 컨트롤러
    @PatchMapping("/{clothesId}")
    public ResponseEntity<ClothesDto> updateClothes(
        @PathVariable("clothesId") UUID clothesId,
        @Valid @RequestPart("request") ClothesUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UUID ownerId = AuthUtil.getAuthenticatedUserId();

        ClothesDto result;
        if (image != null) {
            log.debug(CONTROLLER_NAME + " 이미지 있는 의상 수정 요청 - clothesService.update 호출");
            result = clothesService.update(clothesId, ownerId, request, image);
            log.debug(CONTROLLER_NAME + " 이미지 있는 의상 수정 요청 - 결과 반환: id: {}", result.id());
        }
        else {
            log.debug(CONTROLLER_NAME + " 이미지 없는 의상 수정 요청 - clothesService.update 호출");
            result = clothesService.update(clothesId, ownerId, request);
            log.debug(CONTROLLER_NAME + " 이미지 없는 의상 수정 요청 - 결과 반환: id: {}", result.id());
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }

    // 의상 삭제 요청 컨트롤러
    @DeleteMapping("/{clothesId}")
    public ResponseEntity<Void> deleteClothes (
        @PathVariable("clothesId") UUID clothesId
    ) {
        UUID ownerId = AuthUtil.getAuthenticatedUserId();

        log.debug(CONTROLLER_NAME + " 의상 삭제 요청 - clothesService.delete 호출");
        clothesService.delete(ownerId, clothesId);
        log.debug(CONTROLLER_NAME + " 의상 삭제 요청 - clothesService.delete 종료");

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    // 의상 목록 조회 요청 컨트롤러
    @GetMapping
    public ResponseEntity<CursorResponse<ClothesDto>> getClothes (
        @Valid @ModelAttribute ClothesSearchRequest request
    ) {
        log.debug(CONTROLLER_NAME + " 의상 목록 조회 요청 - clothesService.find 호출");
        CursorResponse<ClothesDto> result = clothesService.find(request);
        log.debug(CONTROLLER_NAME + " 의상 목록 조회 요청 - clothesService.find 종료");

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }
}