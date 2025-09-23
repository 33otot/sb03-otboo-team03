package com.samsamotot.otboo.clothes.service;

import com.samsamotot.otboo.clothes.dto.request.ClothesCreateRequest;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    ClothesDto create(ClothesCreateRequest request);
    ClothesDto create(ClothesCreateRequest request, MultipartFile clothesImage);
}
