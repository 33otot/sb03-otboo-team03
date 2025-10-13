package com.samsamotot.otboo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.dto.event.*;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.sse.service.SseService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * PackageName  : com.samsamotot.otboo.notification.service
 * FileName     : NotificationServiceImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification 서비스 단위 테스트")
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SseService sseService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private User newUserInstance() {
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("User 기본 생성자 접근 실패", e);
        }
    }

    private User newUserWith(String email, UUID id) {
        User u = UserFixture.createUserWithEmail(email);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private void mockAuthUser(User user) {
        var principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "pw", principal.getAuthorities())
        );
    }

    private Notification newNotification(User receiver, Instant createdAt, UUID id, String title) {
        try {
            Constructor<Notification> ctor = Notification.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Notification n = ctor.newInstance();
            ReflectionTestUtils.setField(n, "id", id);
            ReflectionTestUtils.setField(n, "receiver", receiver);
            ReflectionTestUtils.setField(n, "createdAt", createdAt);
            ReflectionTestUtils.setField(n, "title", title);
            ReflectionTestUtils.setField(n, "content", "내용");
            ReflectionTestUtils.setField(n, "level", NotificationLevel.INFO);
            return n;
        } catch (Exception e) {
            throw new RuntimeException("Notification 기본 생성/세팅 실패", e);
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유저_인증한다_실패() {
        // given
        UUID receiverId = UUID.randomUUID();
        given(userRepository.findById(receiverId)).willReturn(Optional.empty());

        // when n then
        OtbooException ex = assertThrows(OtbooException.class,
            () -> notificationService.save(receiverId, "t", "c", NotificationLevel.INFO));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());

        then(notificationRepository).should(never()).save(any());
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    void sse_발행한다_실패() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();

        given(userRepository.findById(receiverId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        willThrow(new RuntimeException("boom"))
            .given(sseService).sendNotification(eq(receiverId), anyString());

        // when n then
        assertDoesNotThrow(() ->
            notificationService.save(receiverId, "title", "content", NotificationLevel.INFO)
        );
        then(notificationRepository).should().save(any(Notification.class));
        then(sseService).should().sendNotification(eq(receiverId), anyString());
    }

    /*
        알림 목록 조회 로직
     */
    @Test
    void receiver_알람들_가져온다() throws Exception {
        // given
        String email = UserFixture.VALID_EMAIL;
        UUID myId = UUID.randomUUID();
        User me = newUserWith(email, myId);

        mockAuthUser(me);
        given(notificationRepository.countByReceiver_Id(myId)).willReturn(2L);

        Instant now = Instant.now();
        Notification n1 = newNotification(me, now.minusSeconds(10), UUID.randomUUID(), "알림1");
        Notification n2 = newNotification(me, now.minusSeconds(20), UUID.randomUUID(), "알림2");

        NotificationRequest req = new NotificationRequest(null, null, 10);

        given(notificationRepository.findLatest(eq(myId), any(Pageable.class)))
            .willReturn(List.of(n1, n2));

        // when
        NotificationListResponse res = notificationService.getNotifications(req);

        // then
        assertNotNull(res);
        assertFalse(res.hasNext());
        assertEquals(2, res.data().size());
        assertEquals(2L, res.totalCount());
        assertEquals("DESCENDING", res.sortDirection());

        then(notificationRepository).should()
            .findLatest(eq(myId), argThat(p -> p.getPageSize() == 11));
    }


    @Test
    void limit_만큼_가져온다() throws Exception {
        // given
        String email = UserFixture.VALID_EMAIL;
        UUID myId = UUID.randomUUID();
        User me = newUserWith(email, myId);

        mockAuthUser(me);
        given(notificationRepository.countByReceiver_Id(myId)).willReturn(3L);

        int limit = 2;
        Instant base = Instant.now();

        Notification a = newNotification(me, base.minusSeconds(1), UUID.randomUUID(), "A"); // 최신
        Notification b = newNotification(me, base.minusSeconds(2), UUID.randomUUID(), "B");
        Notification c = newNotification(me, base.minusSeconds(3), UUID.randomUUID(), "C");

        NotificationRequest req = new NotificationRequest(null, null, limit);

        given(notificationRepository.findLatest(eq(myId), any(Pageable.class)))
            .willReturn(List.of(a, b, c));

        // when
        NotificationListResponse res = notificationService.getNotifications(req);

        // then
        assertNotNull(res);
        assertTrue(res.hasNext());
        assertEquals(limit, res.data().size());

        var last = res.data().get(limit - 1);
        assertEquals(b.getId(), last.getId());
        assertNotNull(res.nextCursor());
        assertNotNull(res.nextIdAfter());

        then(notificationRepository).should()
            .findLatest(eq(myId), argThat(p -> p.getPageSize() == limit + 1));
    }


    @Test
    void cursor_기준_동일하면_보조_커서로_가져온다() throws Exception {
        // given
        String email = UserFixture.VALID_EMAIL;
        UUID myId = UUID.randomUUID();
        User me = newUserWith(email, myId);

        mockAuthUser(me);
        given(notificationRepository.countByReceiver_Id(myId)).willReturn(2L);

        Instant ts = Instant.now();
        UUID idAfter = UUID.randomUUID();
        int limit = 10;

        Notification n1_sameTs_lowerId = newNotification(me, ts, UUID.randomUUID(), "동일시각-낮은ID");
        Notification n2_older = newNotification(me, ts.minusSeconds(1), UUID.randomUUID(), "이전시각");

        given(notificationRepository.findAllBefore(eq(myId), eq(ts), eq(idAfter), any(Pageable.class)))
            .willReturn(List.of(n1_sameTs_lowerId, n2_older));

        NotificationRequest req = new NotificationRequest(ts, idAfter, limit);

        // when
        NotificationListResponse res = notificationService.getNotifications(req);

        // then
        assertNotNull(res);
        assertFalse(res.hasNext());
        assertEquals(2, res.data().size());
        then(notificationRepository).should()
            .findAllBefore(eq(myId), eq(ts), eq(idAfter), argThat(p -> p.getPageSize() == limit + 1));
    }

    @Test
    void 알림_객체_확인한다_실패() throws Exception {
        // given
        String email = UserFixture.VALID_EMAIL;
        UUID myId = UUID.randomUUID();
        User me = newUserWith(email, myId);
        mockAuthUser(me);

        UUID targetNotificationId = UUID.randomUUID();
        given(notificationRepository.findById(targetNotificationId))
            .willReturn(Optional.empty());

        // when
        OtbooException ex = assertThrows(OtbooException.class, () -> notificationService.delete(targetNotificationId));

        // then
        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        then(notificationRepository).should(never()).delete(any(Notification.class));
    }

    @Test
    void 알림_권한_확인한다_실패() throws Exception {
        // given
        String email = UserFixture.VALID_EMAIL;
        UUID myId = UUID.randomUUID();
        User me = newUserWith(email, myId);
        mockAuthUser(me);

        UUID ownerId = UUID.randomUUID();
        User owner = newUserWith("owner@" + email, ownerId);

        UUID notificationId = UUID.randomUUID();
        Notification otherOwnersNotification = newNotification(owner, Instant.now(), notificationId, "다른 사람 알림");

        given(notificationRepository.findById(notificationId))
            .willReturn(Optional.of(otherOwnersNotification));

        // when
        OtbooException ex = assertThrows(OtbooException.class,
            () -> notificationService.delete(notificationId));

        // then
        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        then(notificationRepository).should(never()).delete(any(Notification.class));
    }

    @Test
    void 알림_삭제한다() throws Exception {
        // given
        String email = UserFixture.VALID_EMAIL;
        UUID myId = UUID.randomUUID();
        User me = newUserWith(email, myId);
        mockAuthUser(me);

        UUID notificationId = UUID.randomUUID();
        Notification mine = newNotification(me, Instant.now(), notificationId, "내 알림");

        given(notificationRepository.findById(notificationId))
            .willReturn(Optional.of(mine));

        // when n then
        assertDoesNotThrow(() -> notificationService.delete(notificationId));
        then(notificationRepository).should(times(1)).delete(same(mine));
    }

    /*
        배치 알림 저장 로직
     */
    @Test
    void 배치_알림_저장한다() throws Exception {
        // given
        String title = "테스트 알림";
        String content = "배치 알림 테스트";
        NotificationLevel level = NotificationLevel.INFO;

        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        User user1 = newUserWith("user1@test.com", user1Id);
        User user2 = newUserWith("user2@test.com", user2Id);

        List<UUID> activeUserIds = List.of(user1Id, user2Id);
        List<User> users = List.of(user1, user2);

        given(userRepository.findActiveUserIds()).willReturn(activeUserIds);
        given(userRepository.findAllById(activeUserIds)).willReturn(users);
        given(notificationRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        // when
        assertDoesNotThrow(() -> 
            notificationService.saveBatchNotification(title, content, level)
        );

        // then
        then(userRepository).should().findActiveUserIds();
        then(userRepository).should().findAllById(activeUserIds);
        then(sseService).should(times(2)).sendNotification(any(UUID.class), anyString());
    }

    @Test
    void 배치_알림_저장한다_활성사용자없음() throws Exception {
        // given
        String title = "테스트 알림";
        String content = "배치 알림 테스트";
        NotificationLevel level = NotificationLevel.INFO;

        given(userRepository.findActiveUserIds()).willReturn(List.of());

        // when
        assertDoesNotThrow(() -> 
            notificationService.saveBatchNotification(title, content, level)
        );

        // then
        then(userRepository).should().findActiveUserIds();
        then(userRepository).should(never()).findAllById(anyList());
        then(notificationRepository).should(never()).saveAll(anyList());
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    void 배치_알림_저장한다_SSE실패() throws Exception {
        // given
        String title = "테스트 알림";
        String content = "배치 알림 테스트";
        NotificationLevel level = NotificationLevel.INFO;

        UUID user1Id = UUID.randomUUID();
        User user1 = newUserWith("user1@test.com", user1Id);

        List<UUID> activeUserIds = List.of(user1Id);
        List<User> users = List.of(user1);

        given(userRepository.findActiveUserIds()).willReturn(activeUserIds);
        given(userRepository.findAllById(activeUserIds)).willReturn(users);
        given(notificationRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        willThrow(new RuntimeException("SSE 발행 실패"))
            .given(sseService).sendNotification(any(UUID.class), anyString());

        // when
        assertDoesNotThrow(() -> 
            notificationService.saveBatchNotification(title, content, level)
        );

        // then
        then(userRepository).should().findActiveUserIds();
        then(userRepository).should().findAllById(activeUserIds);
        then(notificationRepository).should().saveAll(anyList());
        then(sseService).should().sendNotification(any(UUID.class), anyString());
    }

    @Test
    void 배치_알림_저장한다_대량사용자() throws Exception {
        // given
        String title = "대량 알림";
        String content = "대량 사용자 테스트";
        NotificationLevel level = NotificationLevel.INFO;

        // 100명의 사용자 생성
        List<UUID> userIds = new java.util.ArrayList<>();
        List<User> users = new java.util.ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            UUID userId = UUID.randomUUID();
            userIds.add(userId);
            users.add(newUserWith("user" + i + "@test.com", userId));
        }

        given(userRepository.findActiveUserIds()).willReturn(userIds);
        given(userRepository.findAllById(userIds)).willReturn(users);
        given(notificationRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        // when
        assertDoesNotThrow(() -> 
            notificationService.saveBatchNotification(title, content, level)
        );

        // then
        then(userRepository).should().findActiveUserIds();
        then(userRepository).should().findAllById(userIds);
        then(sseService).should(times(100)).sendNotification(any(UUID.class), anyString());
    }
}
