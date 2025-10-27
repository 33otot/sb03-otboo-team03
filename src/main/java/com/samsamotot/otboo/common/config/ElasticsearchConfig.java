package com.samsamotot.otboo.common.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

//@Configuration
@EnableElasticsearchRepositories(basePackages = "com.samsamotot.otboo.feed.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String uris;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(uris.split(","))
            .build();
    }

    @Bean
    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(List.of(
            // Date/Time Converters
            new LongToLocalDateTimeConverter(),
            new LocalDateTimeToLongConverter(),
            new StringToLocalDateTimeConverter(),

            // Instant Converters
            new LongToInstantConverter(),
            new InstantToLongConverter(),
            new StringToInstantConverter(),

            // UUID Converters
            new UUIDToStringConverter(),
            new StringToUUIDConverter()
        ));
    }

    /* ==== Converters ==== */

    @ReadingConverter
    public static class LongToLocalDateTimeConverter implements Converter<Long, LocalDateTime> {
        @Override public LocalDateTime convert(Long source) {
            if (source == null) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(source), ZoneOffset.UTC);
        }
    }

    @WritingConverter
    public static class LocalDateTimeToLongConverter implements Converter<LocalDateTime, Long> {
        @Override public Long convert(LocalDateTime source) {
            if (source == null) return null;
            return source.toInstant(ZoneOffset.UTC).toEpochMilli();
        }
    }

    @ReadingConverter
    public static class LongToInstantConverter implements Converter<Long, Instant> {
        @Override public Instant convert(Long source) {
            return source == null ? null : Instant.ofEpochMilli(source);
        }
    }

    @WritingConverter
    public static class InstantToLongConverter implements Converter<Instant, Long> {
        @Override public Long convert(Instant source) {
            return source == null ? null : source.toEpochMilli();
        }
    }

    @ReadingConverter
    public static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override public LocalDateTime convert(String s) {
            if (s == null || s.isBlank()) return null;
            try { return LocalDateTime.ofInstant(Instant.parse(s), ZoneOffset.UTC); }
            catch (Exception e) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
                    return ldt.withNano((ldt.getNano() / 1_000_000) * 1_000_000);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Cannot parse date string: " + s, ex);
                }
            }
        }
    }

    @ReadingConverter
    public static class StringToInstantConverter implements Converter<String, Instant> {
        @Override public Instant convert(String s) {
            if (s == null || s.isBlank()) return null;
            try { return Instant.parse(s); }
            catch (Exception e) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
                    return ldt.toInstant(ZoneOffset.UTC);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Cannot parse instant string: " + s, ex);
                }
            }
        }
    }

    @WritingConverter
    public static class UUIDToStringConverter implements Converter<UUID, String> {
        @Override public String convert(UUID s) { return s == null ? null : s.toString(); }
    }
    @ReadingConverter
    public static class StringToUUIDConverter implements Converter<String, UUID> {
        @Override public UUID convert(String s) { return (s == null || s.isBlank()) ? null : UUID.fromString(s); }
    }
}