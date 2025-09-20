package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 의상 속성 정의 컨트롤러 메서드
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/clothes/attribute-defs")
@RestController
public class ClothesAttributeDefController implements ClothesAttributeDefApi {

    private final ClothesAttributeDefService defService;

    @PostMapping
    public ResponseEntity<ClothesAttributeDefDto> createClothesAttributeDef(
        @Valid @RequestBody ClothesAttributeDefCreateRequest request
    ) {
        log.debug("[ClothesAttributeDefController] createClothesAttributeDef - service.create 호출");
        ClothesAttributeDefDto result = defService.create(request);
        log.debug("[ClothesAttributeDefController] createClothesAttributeDef - service.create 결과: {}", result);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }

    @PatchMapping("/{definitionId}")
    public ResponseEntity<ClothesAttributeDefDto> updateClothesAttributeDef(
        @PathVariable("definitionId") UUID definitionId,
        @Valid @RequestBody ClothesAttributeDefUpdateRequest request
    ) {
        log.debug("[ClothesAttributeDefController] updateClothesAttributeDef - service.update 호출");
        ClothesAttributeDefDto result = defService.update(definitionId, request);
        log.debug("[ClothesAttributeDefController] updateClothesAttributeDef - service.update 결과: {}", result);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }

    @DeleteMapping("/{definitionId}")
    public ResponseEntity<Void> deleteClothesAttributeDef(
        @PathVariable("definitionId") UUID definitionId
    ) {
        log.debug("[ClothesAttributeDefController] deleteClothesAttributeDef - service.delete 호출");
        defService.delete(definitionId);
        log.debug("[ClothesAttributeDefController] deleteClothesAttributeDef - service.delete 종료");

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    @GetMapping
    public ResponseEntity<List<ClothesAttributeDefDto>> getClothesAttributeDef(
        @RequestParam(name = "sortBy") String sortBy,
        @RequestParam(name = "sortDirection") String sortDirection,
        @RequestParam(name = "keywordLike", required = false) String keywordLike
    ) {
        log.debug("[ClothesAttributeDefController] getClothesAttributeDef - service.findAll 호출");
        List<ClothesAttributeDefDto> result = defService.findAll(sortBy, sortDirection, keywordLike);
        log.debug("[ClothesAttributeDefController] getClothesAttributeDef - service.findAll 결과(첫번째 요소): {}", result.get(0));

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }
}
