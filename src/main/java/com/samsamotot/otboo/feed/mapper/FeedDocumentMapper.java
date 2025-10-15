package com.samsamotot.otboo.feed.mapper;

import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.dto.FeedDto;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface FeedDocumentMapper {

    FeedDto toDto(FeedDocument feedDocument);
}
