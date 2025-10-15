package com.samsamotot.otboo.notification.integration;

import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.config.TestConfig;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.notification.integration
 * FileName     : NotificationIntegrationTest
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 */

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import({TestConfig.class, SecurityTestConfig.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("Notification 통합 테스트")
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    private static final String EMAIL = "me@test.com";

    private User me;

    @BeforeEach
    void setUpAuth() {
        me = persistUser(EMAIL);
        var principal = new CustomUserDetails(me);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "pw", principal.getAuthorities())
        );
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private User persistUser(String email) {
        User u = User.builder()
            .email(email)
            .username("tester")
            .password("encoded")
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .isLocked(false)
            .build();
        return userRepository.save(u);
    }

    private Notification newNotification(User receiver, Instant createdAt, String title) {
        try {
            Constructor<Notification> ctor = Notification.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Notification n = ctor.newInstance();
            ReflectionTestUtils.setField(n, "receiver", receiver);
            ReflectionTestUtils.setField(n, "createdAt", createdAt);
            ReflectionTestUtils.setField(n, "title", title);
            ReflectionTestUtils.setField(n, "content", "내용");
            ReflectionTestUtils.setField(n, "level", NotificationLevel.INFO);
            return notificationRepository.save(n);
        } catch (Exception e) {
            throw new RuntimeException("Notification 생성 실패", e);
        }
    }

    @Test
    void limit_만큼_가져온다() throws Exception {
        // given
        Instant base = Instant.now();
        newNotification(me, base.minusSeconds(3), "N3");
        newNotification(me, base.minusSeconds(2), "N2");
        newNotification(me, base.minusSeconds(1), "N1");

        mockMvc.perform(get("/api/notifications").param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.hasNext", is(true)))
            .andExpect(jsonPath("$.totalCount", is(3)))
            .andExpect(jsonPath("$.sortDirection", is("DESCENDING")));
    }

    @Test
    void 없으면_안_가져온다() throws Exception {
        // given

        // when n then
        mockMvc.perform(get("/api/notifications").param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)))
            .andExpect(jsonPath("$.hasNext", is(false)))
            .andExpect(jsonPath("$.totalCount", is(0)));
    }

}
