package com.samsamotot.otboo.common.oauth2.dto;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
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
        Object id = attributes.get("id");
        if (id == null) {
            throw new OtbooException(ErrorCode.INVALID_OAUTH2_USER_INFO);
        }
        return id.toString();
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
        return null;
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
