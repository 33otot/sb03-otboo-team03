package com.samsamotot.otboo.oauth2.dto;

import java.util.Map;

/**
 * Kakao OAuth2 사용자 정보 DTO
 */
public class KakaoOAuth2UserInfoDto extends OAuth2UserInfoDto {

    public KakaoOAuth2UserInfoDto(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        if (properties == null) {
            return null;
        }

        return (String) properties.get("nickname");
    }

    @Override
    public String getEmail() {
        return "";
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        if (properties == null) {
            return null;
        }

        return (String) properties.get("profile_image");
    }
}
