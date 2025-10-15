package com.samsamotot.otboo.feed.document;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Builder
@Document(indexName = "feeds", createIndex = false)
@Setting(settingPath = "elasticsearch/feed-settings.json")
@Mapping(mappingPath = "elasticsearch/feed-mapping.json")
public record FeedDocument(
    @Id
    UUID id,
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    Instant createdAt,
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    Instant updatedAt,
    AuthorDto author,
    WeatherDto weather,
    List<OotdDto> ootds,
    String content,
    long likeCount,
    long commentCount,
    boolean isDeleted
) {
}