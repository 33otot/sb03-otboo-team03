package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 의상 속성 정의 Springdoc 어노테이션 매핑
 */
@Tag(name = "의상 속성 정의", description = "의상 속성 정의 관련 API")
public interface ClothesAttributeDefApi {

    @Operation(summary = "의상 속성 정의 등록", description = "의상 속성 정의 등록 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "201", description = "의상 속성 정의 등록 성공",
                content = @Content(schema = @Schema(implementation = ClothesAttributeDefDto.class))),
            @ApiResponse(responseCode = "400", description = "의상 속성 정의 등록 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping
    ResponseEntity<ClothesAttributeDefDto> createClothesAttributeDef(
        @Parameter ClothesAttributeDefCreateRequest request
    );

    @Operation(summary = "의상 속성 정의 수정", description = "의상 속성 정의 수정 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "의상 속성 정의 수정 성공",
                content = @Content(schema = @Schema(implementation = ClothesAttributeDefDto.class))),
            @ApiResponse(responseCode = "400", description = "의상 속성 정의 수정 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PatchMapping("/{definitionId}")
    ResponseEntity<ClothesAttributeDefDto> updateClothesAttributeDef(
        @PathVariable UUID definitionId,
        @Parameter ClothesAttributeDefUpdateRequest request
    );

    @Operation(summary = "의상 속성 정의 삭제", description = "의상 속성 정의 삭제 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "204", description = "의상 속성 정의 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "의상 속성 정의 삭제 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @DeleteMapping("/{definitionId}")
    ResponseEntity<Void> deleteClothesAttributeDef(
        @PathVariable UUID definitionId
    );

    @Operation(summary = "의상 속성 정의 목록 조회", description = "의상 속성 정의 목록 조회 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "의상 속성 정의 목록 조회 성공",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClothesAttributeDefDto.class)))),
            @ApiResponse(responseCode = "400", description = "의상 속성 정의 목록 조회제 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping
    ResponseEntity<List<ClothesAttributeDefDto>> getClothesAttributeDef(
        @RequestParam(name = "sortBy") String sortBy,
        @RequestParam(name = "sortDirection") String sortDirection,
        @RequestParam(name = "keywordLike", required = false) String keywordLike
    );
}
