package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import com.samsamotot.otboo.clothes.service.ClothesAttributeDefService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/clothes/attribute-defs")
@RestController
public class ClothesAttributeDefController implements ClothesAttributeDefApi {

    private final ClothesAttributeDefService defService;

    @PostMapping
    public ResponseEntity<ClothesAttributeDefDto> createClothesAttributeDef(
        @Valid @RequestBody ClothesAttributeDefCreateRequest request
    ) {
        ClothesAttributeDefDto result = defService.create(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result);
    }

    @PatchMapping("/{definitionId}")
    public ResponseEntity<ClothesAttributeDefDto> updateClothesAttributeDef(
        @PathVariable("definitionId") UUID definitionId,
        @Valid @RequestBody ClothesAttributeDefUpdateRequest request
    ) {
        ClothesAttributeDefDto result = defService.update(definitionId, request);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }

    @DeleteMapping("/{definitionId}")
    public ResponseEntity<ClothesAttributeDefDto> deleteClothesAttributeDef(
        @PathVariable("definitionId") UUID definitionId
    ) {
        defService.delete(definitionId);

        return ResponseEntity
            .noContent()
            .build();
    }
}
