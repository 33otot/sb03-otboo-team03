package com.samsamotot.otboo.oauth2.dto;

import com.samsamotot.otboo.common.exception.OAuth2AuthenticationProcessingException;
import com.samsamotot.otboo.user.entity.Provider;
import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfoDto getOAuth2UserInfo(Provider provider, Map<String, Object> attributes) {

        switch (provider) {
            case GOOGLE:
                return new GoogleOAuthUserInfoDto(attributes);
            case KAKAO:
                return new KakaoOAuth2UserInfoDto(attributes);
            default:
                throw new OAuth2AuthenticationProcessingException("INVALID PROVIDER TYPE");
        }
    }
}
