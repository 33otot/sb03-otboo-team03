package com.samsamotot.otboo.common.oauth2.dto;

import java.util.Map;

/**
 * OAuth2 사용자 정보 DTO 추상 클래스
 */
public abstract class OAuth2UserInfoDto {
    protected Map<String, Object> attributes;

    public OAuth2UserInfoDto(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();

    public abstract String getName();

    public abstract String getEmail();

    public abstract String getImageUrl();

    public boolean isEmailVerified() {
        Object verified = attributes.get("email_verified");
        if (verified instanceof Boolean) {
            return (Boolean) verified;
        }
        if (verified instanceof String) {
            return Boolean.parseBoolean((String) verified);
        }
        return false;
    }
}
