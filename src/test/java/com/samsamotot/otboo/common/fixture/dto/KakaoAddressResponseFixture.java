package com.samsamotot.otboo.common.fixture.dto;

import com.samsamotot.otboo.location.dto.KakaoAddressResponse;

import java.util.List;

public class KakaoAddressResponseFixture {

    public static KakaoAddressResponse createKakaoAddressResponse() {
        KakaoAddressResponse.Document document = new KakaoAddressResponse.Document();
        document.setRegionType("H"); // 행정동
        document.setRegion1DepthName("서울특별시");
        document.setRegion2DepthName("중구");
        document.setRegion3DepthName("명동");
        document.setRegion4DepthName("");
        document.setX(60.0);
        document.setY(127.0);

        KakaoAddressResponse response = new KakaoAddressResponse();
        response.setDocuments(List.of(document));

        return response;
    }

    public static KakaoAddressResponse createEmptyResponse() {
        KakaoAddressResponse response = new KakaoAddressResponse();
        response.setDocuments(List.of());
        return response;
    }

    public static KakaoAddressResponse createNullResponse() {
        return null;
    }
}
