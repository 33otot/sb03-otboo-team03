package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "의상 속성 정의", description = "의상 속성 정의 관련 API")
public interface ClothesAttributeDefApi {

    @Operation(summary = "의상 속성 정의 등록")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "201", description = "의상 속성 정의 등록 성공",
                content = @Content(schema = @Schema(implementation = ClothesAttributeDefDto.class))),
            @ApiResponse(responseCode = "400", description = "의상 속성 정의 등록 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    ResponseEntity<ClothesAttributeDefDto> createClothesAttributeDef(
        @Parameter ClothesAttributeDefCreateRequest request
    );

}
