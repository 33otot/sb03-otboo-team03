package com.samsamotot.otboo.clothes.controller;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import com.samsamotot.otboo.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "옷 수정", description = "옷 수정 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "옷 수정 성공",
                content = @Content(schema = @Schema(implementation = ClothesDto.class))),
            @ApiResponse(responseCode = "400", description = "옷 수정 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PatchMapping("/{clothesId}")
    ResponseEntity<ClothesDto> updateClothes(
        @PathVariable UUID clothesId,
        @Parameter ClothesUpdateRequest request,
        @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) MultipartFile image
    );

    @Operation(summary = "옷 삭제", description = "옷 삭제 API")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "204", description = "옷 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "옷 삭제 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "옷 찾기 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @DeleteMapping("/{clothesId}")
    ResponseEntity<Void> deleteClothes(
        @PathVariable UUID clothesId
    );
}
