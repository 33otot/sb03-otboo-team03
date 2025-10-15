package com.samsamotot.otboo.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.common.exception.OAuth2AuthenticationProcessingException;
import com.samsamotot.otboo.common.oauth2.principal.OAuth2UserPrincipal;
import com.samsamotot.otboo.common.oauth2.service.OAuth2UserService;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2User 서비스 단위 테스트")
public class OAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private OAuth2UserService oAuth2UserService;

    private ClientRegistration googleRegistration;
    private ClientRegistration kakaoRegistration;

    @BeforeEach
    void setUp() {
        googleRegistration = ClientRegistration.withRegistrationId("google")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("test-client")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
            .userNameAttributeName("sub")
            .redirectUri("http://localhost/login/oauth2/code/google")
            .build();

        kakaoRegistration = ClientRegistration.withRegistrationId("kakao")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("test-client")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token")
            .userInfoUri("https://kapi.kakao.com/v2/user/me")
            .userNameAttributeName("id")
            .redirectUri("http://localhost/login/oauth2/code/kakao")
            .build();
    }

    private OAuth2AccessToken testAccessToken() {
        return new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-access-token",
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(3600),
            Set.of("profile", "email")
        );
    }

    private OAuth2UserRequest req(ClientRegistration reg) {
        return new OAuth2UserRequest(reg, testAccessToken());
    }

    private OAuth2User googleUser(String sub, String email, String name, String picture) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", sub);
        attrs.put("email", email);
        attrs.put("name", name);
        attrs.put("picture", picture);
        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attrs,
            "sub"
        );
    }

    @SuppressWarnings("unchecked")
    private OAuth2User kakaoUser(String id, String nickname, String imageUrl) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", nickname);
        properties.put("profile_image", imageUrl);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", id);
        attrs.put("properties", properties);

        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attrs,
            "id"
        );
    }

    @Nested
    @DisplayName("Kakao 연동 테스트")
    class KakaoTests {

        @Test
        void 기존_유저가_있으면_업데이트만_수행한다() {
            // given
            String kakaoId = "12345";
            String nickname = "홍길동";
            String imageUrl = "http://img";
            OAuth2User oAuth2User = kakaoUser(kakaoId, nickname, imageUrl);
            OAuth2UserRequest request = req(kakaoRegistration);

            User existing = User.builder()
                .provider(Provider.KAKAO)
                .providerId(kakaoId)
                .username("old")
                .email("old@kakao.com")
                .role(Role.USER)
                .build();
            ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());

            when(userRepository.findByProviderAndProviderId(Provider.KAKAO, kakaoId))
                .thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // when
            OAuth2User principal = oAuth2UserService.processOAuth2User(request, oAuth2User);

            // then
            verify(userRepository).findByProviderAndProviderId(Provider.KAKAO, kakaoId);
            verify(userRepository).save(existing);
            verify(profileRepository, never()).save(any(Profile.class));

            assertThat(principal).isInstanceOf(OAuth2UserPrincipal.class);
            assertThat(((OAuth2UserPrincipal) principal).getId()).isEqualTo(existing.getId());
        }

        @Test
        void 기존_유저가_없으면_가상_이메일로_유저와_프로필을_생성한다() {
            // given
            String kakaoId = "99999";
            String nickname = "테스트사용자";
            String imageUrl = "http://img2";
            OAuth2User oAuth2User = kakaoUser(kakaoId, nickname, imageUrl);
            OAuth2UserRequest request = req(kakaoRegistration);

            when(userRepository.findByProviderAndProviderId(Provider.KAKAO, kakaoId))
                .thenReturn(Optional.empty());
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);

            UUID newId = UUID.randomUUID();
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User userToSave = inv.getArgument(0);
                ReflectionTestUtils.setField(userToSave, "id", newId);
                return userToSave;
            });

            when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            OAuth2User principal = oAuth2UserService.processOAuth2User(request, oAuth2User);

            // then
            verify(userRepository).findByProviderAndProviderId(Provider.KAKAO, kakaoId);
            verify(userRepository).existsByEmail(anyString());
            verify(userRepository).save(userCaptor.capture());
            verify(profileRepository).save(profileCaptor.capture());

            User saved = userCaptor.getValue();
            Profile savedProfile = profileCaptor.getValue();

            assertThat(saved.getProvider()).isEqualTo(Provider.KAKAO);
            assertThat(saved.getProviderId()).isEqualTo(kakaoId);
            assertThat(saved.getEmail()).endsWith("@kakao.com");
            assertThat(savedProfile.getUser()).isNotNull();
            assertThat(savedProfile.getName()).isEqualTo(nickname);

            assertThat(((OAuth2UserPrincipal) principal).getId()).isEqualTo(newId);
        }
    }

    @Nested
    @DisplayName("Google 연동 테스트")
    class GoogleTests {

        @Test
        void 기존_유저가_있으면_업데이트만_수행한다() {
            String sub = "sub-3";
            String email = "exist@example.com";
            String name = "Carol";
            OAuth2User oAuth2User = googleUser(sub, email, name, null);
            OAuth2UserRequest request = req(googleRegistration);

            User existing = User.builder()
                .provider(Provider.GOOGLE)
                .providerId(sub)
                .username("old")
                .email(email)
                .role(Role.USER)
                .build();
            ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            oAuth2UserService.processOAuth2User(request, oAuth2User);

            verify(userRepository).findByEmail(email);
            verify(userRepository).save(existing);
            verify(profileRepository, never()).save(any());
        }

        @Test
        void 기존_유저가_없으면_유저와_프로필을_생성한다() {
            // given
            String sub = "sub-1";
            String email = "user@example.com";
            String name = "Alice";
            String picture = "http://p";

            OAuth2User oAuth2User = googleUser(sub, email, name, picture);
            OAuth2UserRequest request = req(googleRegistration);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
                return u;
            });
            when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            OAuth2User principal = oAuth2UserService.processOAuth2User(request, oAuth2User);

            // then
            verify(userRepository).findByEmail(email);
            verify(userRepository).save(any(User.class));
            verify(profileRepository).save(any(Profile.class));
            assertThat(principal).isInstanceOf(OAuth2UserPrincipal.class);
        }

        @Test
        void 이메일이_없으면_예외가_발생한다() {
            // given
            String sub = "sub-2";
            OAuth2User oAuth2User = googleUser(sub, null, "Bob", null);
            OAuth2UserRequest request = req(googleRegistration);

            // when & then
            assertThatThrownBy(() -> oAuth2UserService.processOAuth2User(request, oAuth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("Email not found");
        }
    }
}
