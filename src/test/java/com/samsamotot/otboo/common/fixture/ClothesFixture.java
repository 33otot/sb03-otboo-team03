package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.user.entity.User;
import java.util.List;

public class ClothesFixture {

    public static final String DEFAULT_CLOTHES_NAME = "T-Shirt";
    public static final ClothesType DEFAULT_CLOTHES_TYPE = ClothesType.TOP;

    public static Clothes createClothes() {
        return Clothes.builder()
            .name(DEFAULT_CLOTHES_NAME)
            .type(DEFAULT_CLOTHES_TYPE)
            .attributes(List.of())
            .build();
    }

    public static Clothes createClothes(ClothesType clothesType) {
        return Clothes.builder()
            .name(DEFAULT_CLOTHES_NAME)
            .type(clothesType)
            .attributes(List.of())
            .build();
    }

    public static Clothes createClothes(User user) {
        return Clothes.builder()
            .name(DEFAULT_CLOTHES_NAME)
            .type(DEFAULT_CLOTHES_TYPE)
            .owner(user)
            .attributes(List.of())
            .build();
    }
}
