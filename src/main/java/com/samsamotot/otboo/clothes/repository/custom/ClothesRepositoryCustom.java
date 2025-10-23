package com.samsamotot.otboo.clothes.repository.custom;

import com.samsamotot.otboo.clothes.dto.request.ClothesSearchRequest;
import com.samsamotot.otboo.clothes.entity.Clothes;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ClothesRepositoryCustom {
    // 목록 조회
    Slice<Clothes> findClothesWithCursor(UUID ownerId, ClothesSearchRequest request, Pageable pageable);

    // totalElement 구하기
    long totalElementCount(UUID ownerId, ClothesSearchRequest request);
}
