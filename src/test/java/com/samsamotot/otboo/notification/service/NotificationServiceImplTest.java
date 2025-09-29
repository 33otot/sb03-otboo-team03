package com.samsamotot.otboo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.sse.service.SseService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
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
@DisplayName("SSE 서비스 단위 테스트")
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

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private User newUserInstance() {
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("User 기본 생성자 접근 실패", e);
        }
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

    @Test
    void 권한_변경_객체_생성() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        notificationService.notifyRole(userId);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification n = notificationCaptor.getValue();
        assertEquals("권한 변경", n.getTitle());
        assertEquals("변한 변경 확인", n.getContent());
        assertEquals(NotificationLevel.INFO, n.getLevel());
        assertNotNull(n.getReceiver());
        then(sseService).should().sendNotification(eq(userId), anyString());
    }

    @Test
    void 의상_속성_추가_객체_생성() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        notificationService.notifyClothesAttrbute(userId);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification n = notificationCaptor.getValue();
        assertEquals("의상 속성 추가", n.getTitle());
        assertEquals("의상 속성이 추가 되었습니다.", n.getContent());
        assertEquals(NotificationLevel.INFO, n.getLevel());
        assertNotNull(n.getReceiver());
        then(sseService).should().sendNotification(eq(userId), anyString());
    }

    @Test
    void 새_좋아요_객체_생성() throws Exception {
        UUID commenterId = UUID.randomUUID();
        UUID feedOwnerId = UUID.randomUUID();
        given(userRepository.findById(feedOwnerId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        notificationService.notifyLike(commenterId, feedOwnerId);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification n = notificationCaptor.getValue();
        assertEquals("새 좋아요", n.getTitle());
        assertTrue(n.getContent().contains(commenterId.toString()));
        assertNotNull(n.getReceiver());
        then(sseService).should().sendNotification(eq(feedOwnerId), anyString());
    }

    @Test
    void 새_댓글_객체_생성() throws Exception {
        UUID commenterId = UUID.randomUUID();
        UUID feedOwnerId = UUID.randomUUID();
        String preview = "멋지네요!";
        given(userRepository.findById(feedOwnerId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        notificationService.notifyComment(commenterId, feedOwnerId, preview);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification n = notificationCaptor.getValue();
        assertEquals("새 댓글", n.getTitle());
        assertTrue(n.getContent().contains(commenterId.toString()));
        assertTrue(n.getContent().contains(preview));
        assertNotNull(n.getReceiver());
        then(sseService).should().sendNotification(eq(feedOwnerId), anyString());
    }

    @Test
    void 새_팔로워_객체_생성() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        given(userRepository.findById(followeeId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        notificationService.notifyFollow(followerId, followeeId);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification n = notificationCaptor.getValue();
        assertEquals("새 팔로워", n.getTitle());
        assertEquals("사용자가 팔로우했습니다", n.getContent());
        assertEquals(NotificationLevel.INFO, n.getLevel());
        assertNotNull(n.getReceiver());
        then(sseService).should().sendNotification(eq(followeeId), anyString());
    }

    @Test
    void 새_쪽지_객체_생성() throws Exception {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        String preview = "안녕!";
        given(userRepository.findById(receiverId)).willReturn(Optional.of(newUserInstance()));
        given(notificationRepository.save(any(Notification.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        notificationService.notifyDirectMessage(senderId, receiverId, preview);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification n = notificationCaptor.getValue();
        assertEquals("새 쪽지", n.getTitle());
        assertTrue(n.getContent().contains(senderId.toString()));
        assertTrue(n.getContent().contains(preview));
        assertNotNull(n.getReceiver());
        then(sseService).should().sendNotification(eq(receiverId), anyString());
    }
}
