package com.samsamotot.otboo.clothes.mapper;

import com.samsamotot.otboo.clothes.dto.ClothesAttributeWithDefDto;
import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesAttribute;
import com.samsamotot.otboo.clothes.entity.ClothesAttributeOption;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesMapper {

    @Mapping(source = "clothes.id", target = "id")
    @Mapping(source = "clothes.owner.id", target = "ownerId")
    @Mapping(source = "clothes.name", target = "name")
    @Mapping(source = "clothes.imageUrl", target = "imageUrl")
    @Mapping(source = "clothes.type", target = "type")
    @Mapping(source = "clothes.attributes", target = "attributes")
    ClothesDto toClothesDto(Clothes clothes);

    @Mapping(source = "clothes.id", target = "clothesId")
    @Mapping(source = "clothes.name", target = "name")
    @Mapping(source = "clothes.imageUrl", target = "imageUrl")
    @Mapping(source = "clothes.type", target = "type")
    @Mapping(source = "clothes.attributes", target = "attributes")
    OotdDto toOotdDto(FeedClothes feedClothes);

    @Mapping(source = "clothes.id", target = "clothesId")
    @Mapping(source = "clothes.name", target = "name")
    @Mapping(source = "clothes.imageUrl", target = "imageUrl")
    @Mapping(source = "clothes.type", target = "type")
    @Mapping(source = "clothes.attributes", target = "attributes")
    OotdDto toOotdDto(Clothes clothes);

    @Mapping(source = "definition.id", target = "definitionId")
    @Mapping(source = "definition.name", target = "definitionName")
    @Mapping(target = "selectableValues", expression = "java(mapOptionsToSelectableValues(attribute))")
    ClothesAttributeWithDefDto toClothesAttributeWithDefDto(ClothesAttribute attribute);

    default List<String> mapOptionsToSelectableValues(ClothesAttribute attribute) {
        if (attribute == null || attribute.getDefinition() == null) {
            return null;
        }
        List<ClothesAttributeOption> options = attribute.getDefinition().getOptions();
        if (options == null) {
            return null;
        }
        return options.stream()
            .map(ClothesAttributeOption::getValue)
            .toList();
    }
}
