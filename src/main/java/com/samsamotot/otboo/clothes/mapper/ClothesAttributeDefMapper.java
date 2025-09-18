package com.samsamotot.otboo.clothes.mapper;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeDefDto;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeDef;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesAttributeDefMapper {

    @Mapping(target = "selectableValues", expression = "java(mapOptions(entity.getOptions()))")
    ClothesAttributeDefDto toDto(ClothesAttributeDef entity);

    default List<String> mapOptions(List<ClothesAttributeOption> options) {
        return options.stream()
            .map(ClothesAttributeOption::getValue)
            .toList();
    }
}
