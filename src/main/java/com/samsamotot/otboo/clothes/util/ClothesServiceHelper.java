package com.samsamotot.otboo.clothes.util;

import com.samsamotot.otboo.clothes.entity.Clothes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClothesServiceHelper {

    // 커서 생성 (생성 시간)
    public String generateCursor(List<Clothes> clothes) {
        if (clothes.isEmpty()) return null;

        Clothes lastClothes = clothes.get(clothes.size() - 1);

        return lastClothes.getCreatedAt().toString();
    }
}
