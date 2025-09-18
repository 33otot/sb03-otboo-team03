package com.samsamotot.otboo.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KakaoAddressResponse {
    private Meta meta;
    private List<Document> documents;

    @Data
    public static class Meta {
        @JsonProperty("total_count")
        private int totalCount;
    }

    @Data
    public static class Document {
        @JsonProperty("region_type")
        private String regionType; // H(행정동) 또는 B(법정동)

        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("region_1depth_name")
        private String region1DepthName; // 시/도

        @JsonProperty("region_2depth_name")
        private String region2DepthName; // 시/군/구

        @JsonProperty("region_3depth_name")
        private String region3DepthName; // 읍/면/동

        @JsonProperty("region_4depth_name")
        private String region4DepthName; // 리/동

        private String code;
        private double x;
        private double y;
    }

}
