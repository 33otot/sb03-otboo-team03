package com.samsamotot.otboo.clothes.service;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import com.samsamotot.otboo.common.dto.CursorResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    // create
    ClothesDto create(UUID ownerId, ClothesCreateRequest request);
    ClothesDto create(UUID ownerId, ClothesCreateRequest request, MultipartFile clothesImage);

    // update
    ClothesDto update(UUID clothesId, UUID ownerId, ClothesUpdateRequest updateRequest);
    ClothesDto update(UUID clothesId, UUID ownerId, ClothesUpdateRequest updateRequest, MultipartFile clothesImage);

    // delete
    void delete(UUID ownerId, UUID clothesId);

    // read
    CursorResponse<ClothesDto> find(UUID ownerId, ClothesSearchRequest request);
}
