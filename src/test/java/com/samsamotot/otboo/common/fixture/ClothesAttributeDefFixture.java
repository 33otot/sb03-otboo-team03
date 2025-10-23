package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import java.time.Instant;
import java.util.List;

/**
 * 의상 속성 정의 Fixture 클래스
 */
public class ClothesAttributeDefFixture {

    private static final String name = "계절";
    private static final List<String> options = List.of("봄", "여름", "가을", "겨울");

    public static ClothesAttributeDef createClothesAttributeDef(){
        return ClothesAttributeDef.createClothesAttributeDef(name, options);
    }

    public static ClothesAttributeDef createClothesAttributeDef(String customName, List<String> customOptions){
        return ClothesAttributeDef.createClothesAttributeDef(customName, customOptions);
    }
}
