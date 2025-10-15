package com.samsamotot.otboo.common.oauth2.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.oauth2.dto.OAuth2UserInfoDto;
import com.samsamotot.otboo.common.oauth2.dto.OAuth2UserInfoFactory;
import com.samsamotot.otboo.common.oauth2.principal.OAuth2UserPrincipal;
import com.samsamotot.otboo.common.oauth2.util.KakaoEmailFactory;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * OAuth2 사용자 정보를 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final String SERVICE = "[OAuth2UserService] ";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    /**
     * OAuth2UserRequest를 기반으로 OAuth2User를 로드하고 처리합니다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {

        log.debug(SERVICE + "OAuth2User 로드 시작 - ClientRegistration: {}", oAuth2UserRequest.getClientRegistration().getRegistrationId());

        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * OAuth2 사용자 정보를 처리합니다.
     * 기존 사용자가 있으면 업데이트, 없으면 새로 등록합니다.
     */
    @Transactional
    public OAuth2User processOAuth2User(OAuth2UserRequest req, OAuth2User oAuth2User) {

        log.debug(SERVICE + "OAuth2 사용자 정보 처리 시작");
        // 표준화된 사용자 정보
        Provider provider = Provider.valueOf(
            req.getClientRegistration().getRegistrationId().trim().toUpperCase(java.util.Locale.ROOT)
        );

        OAuth2UserInfoDto info = OAuth2UserInfoFactory.getOAuth2UserInfo(
            provider,
            oAuth2User.getAttributes()
        );

        String providerUserId = info.getId();
        String name = info.getName();
        String imageUrl = info.getImageUrl();

        User user = userRepository.findByProviderAndProviderId(provider, providerUserId)
            .map(u -> updateExistingUser(u, info))
            .orElse(null);

        if (user != null) {
            return OAuth2UserPrincipal.create(user, oAuth2User.getAttributes());
        }

        if (provider == Provider.KAKAO) {
            // 가상 이메일 생성 (카카오는 이메일이 없을 수 있음)
            final String generatedEmail =
                KakaoEmailFactory.generate(name, providerUserId, userRepository::existsByEmail);
            user = registerNewUser(provider, providerUserId, name, imageUrl, generatedEmail);
        } else {
            // 그 외는 이메일로 식별
            final String email = info.getEmail();
            if (!StringUtils.hasText(email)) {
                throw new OtbooException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
            }

            // 구글은 이메일 검증 여부도 체크 (email_verified)
            if (provider == Provider.GOOGLE) {
                Object ev = info.getAttributes().get("email_verified");
                if (!isEmailVerified(ev)) {
                    throw new OtbooException(ErrorCode.OAUTH2_EMAIL_NOT_VERIFIED);
                }
            }

            user = userRepository.findByEmail(email)
                .map(u -> updateExistingUser(u, info))
                .orElseGet(() -> registerNewUser(provider, providerUserId, name, imageUrl, email));
        }

        return OAuth2UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    /**
     * 새로운 사용자를 등록합니다.
     */
    private User registerNewUser(Provider provider, String providerUserId, String name, String imageUrl, String email) {

        // User 및 Profile 생성
        User user = User.builder()
            .provider(provider)
            .providerId(providerUserId)
            .username(name)
            .email(email)
            .role(Role.USER)
            .build();
        User savedUser = userRepository.save(user);

        Profile profile = Profile.builder()
            .user(savedUser)
            .name(name)
            .profileImageUrl(imageUrl)
            .build();
        profileRepository.save(profile);

        return savedUser;
    }

    /**
     * 기존 사용자의 정보를 업데이트합니다.
     */
    private User updateExistingUser(User existing, OAuth2UserInfoDto info) {

        if (StringUtils.hasText(info.getName())) {
            existing.updateUserInfo(info.getName());
        }
        return userRepository.save(existing);
    }

    private boolean isEmailVerified(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}
