package com.samsamotot.otboo.clothes.service;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    // create
    ClothesDto create(ClothesCreateRequest request);
    ClothesDto create(ClothesCreateRequest request, MultipartFile clothesImage);

    // update
    ClothesDto update(UUID clothesId, ClothesUpdateRequest updateRequest);
    ClothesDto update(UUID clothesId, ClothesUpdateRequest updateRequest, MultipartFile clothesImage);

    // delete
    void delete(UUID clothesId);
}
