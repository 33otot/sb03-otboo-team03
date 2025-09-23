package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 관리 Springdoc 어노테이션 매핑
 */
@Tag(name = "의상 관리", description = "의상 관리 API")
public interface ClothesControllerApi {

    @Operation(summary = "옷 등록", description = "옷 등록 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "201", description = "옷 등록 성공",
                content = @Content(schema = @Schema(implementation = ClothesDto.class))),
            @ApiResponse(responseCode = "400", description = "옷 등록 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping
    ResponseEntity<ClothesDto> createClothes(
        @Parameter ClothesCreateRequest request,
        @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) MultipartFile image
    );
}
