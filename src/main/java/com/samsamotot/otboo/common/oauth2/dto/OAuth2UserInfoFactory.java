package com.samsamotot.otboo.common.oauth2.dto;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
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
                throw new OtbooException(ErrorCode.INVALID_OAUTH2_PROVIDER);
        }
    }
}
