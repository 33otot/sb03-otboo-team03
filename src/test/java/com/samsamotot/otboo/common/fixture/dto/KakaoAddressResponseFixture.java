package com.samsamotot.otboo.common.fixture.dto;

import com.samsamotot.otboo.location.dto.KakaoAddressResponse;

import java.util.List;

public class KakaoAddressResponseFixture {

    /**
     * 정상적인 카카오 API 응답 (행정동 포함)
     */
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

    /**
     * 빈 응답 (문서가 없는 경우)
     */
    public static KakaoAddressResponse createEmptyResponse() {
        KakaoAddressResponse response = new KakaoAddressResponse();
        response.setDocuments(List.of());
        return response;
    }

    public static KakaoAddressResponse createNullResponse() {
        return null;
    }

    /**
     * 행정동과 법정동이 모두 있는 응답 (행정동 우선 선택 테스트용)
     */
    public static KakaoAddressResponse createResponseWithAdministrativeDistrict() {
        KakaoAddressResponse.Document adminDoc = new KakaoAddressResponse.Document();
        adminDoc.setRegionType("H"); // 행정동
        adminDoc.setRegion1DepthName("서울특별시");
        adminDoc.setRegion2DepthName("중구");
        adminDoc.setRegion3DepthName("명동");
        adminDoc.setRegion4DepthName("");
        adminDoc.setX(60.0);
        adminDoc.setY(127.0);

        KakaoAddressResponse.Document legalDoc = new KakaoAddressResponse.Document();
        legalDoc.setRegionType("B"); // 법정동
        legalDoc.setRegion1DepthName("서울특별시");
        legalDoc.setRegion2DepthName("중구");
        legalDoc.setRegion3DepthName("을지로");
        legalDoc.setRegion4DepthName("");
        legalDoc.setX(61.0);
        legalDoc.setY(128.0);

        KakaoAddressResponse response = new KakaoAddressResponse();
        response.setDocuments(List.of(adminDoc, legalDoc));

        return response;
    }

    /**
     * 법정동만 있는 응답 (행정동이 없는 경우)
     */
    public static KakaoAddressResponse createResponseWithoutAdministrativeDistrict() {
        KakaoAddressResponse.Document legalDoc = new KakaoAddressResponse.Document();
        legalDoc.setRegionType("B"); // 법정동만
        legalDoc.setRegion1DepthName("서울특별시");
        legalDoc.setRegion2DepthName("중구");
        legalDoc.setRegion3DepthName("명동");
        legalDoc.setRegion4DepthName("");
        legalDoc.setX(60.0);
        legalDoc.setY(127.0);

        KakaoAddressResponse response = new KakaoAddressResponse();
        response.setDocuments(List.of(legalDoc));

        return response;
    }
}
