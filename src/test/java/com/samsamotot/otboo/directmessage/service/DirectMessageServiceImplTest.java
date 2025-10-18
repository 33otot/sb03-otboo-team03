package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomListResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.dto.SendDmRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.mapper.DirectMessageMapper;
import com.samsamotot.otboo.directmessage.repository.DirectMessageRepository;
import com.samsamotot.otboo.notification.dto.event.DirectMessageReceivedEvent;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.dto.AuthorDto;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : DirectMessageServiceImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("Direct Message 서비스 단위 테스트")
class DirectMessageServiceImplTest {

    @InjectMocks
    private DirectMessageServiceImpl directMessageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private jakarta.persistence.EntityManager em;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private DirectMessageMapper directMessageMapper;

    @Captor
    private ArgumentCaptor<DirectMessage> dmCaptor;

    @Captor
    private ArgumentCaptor<DirectMessageReceivedEvent> eventCaptor;

    private final String myEmail = "me@example.com";
    private final UUID myId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private final UUID otherId = UUID.fromString("a0000000-0000-0000-0000-000000000002");


    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAsUserId(UUID userId, String email) {
        CustomUserDetails cud = mock(CustomUserDetails.class);
        when(cud.getId()).thenReturn(userId);
        when(cud.getUsername()).thenReturn(email);
        var auth = new UsernamePasswordAuthenticationToken(cud, "N/A", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static DirectMessage savedDmMock(UUID id, Instant createdAt, DirectMessage from) {
        DirectMessage m = mock(DirectMessage.class);
        lenient().when(m.getId()).thenReturn(id);
        lenient().when(m.getCreatedAt()).thenReturn(createdAt);
        lenient().when(m.getSender()).thenReturn(from.getSender());
        lenient().when(m.getReceiver()).thenReturn(from.getReceiver());
        lenient().when(m.getMessage()).thenReturn(from.getMessage());
        return m;
    }

    private void commonGiven(User senderRef, User receiverRef, DirectMessageDto dtoToReturn) {
        given(em.getReference(User.class, myId)).willReturn(senderRef);
        given(em.getReference(User.class, otherId)).willReturn(receiverRef);

        willAnswer(inv -> {
            DirectMessage toSave = inv.getArgument(0);
            DirectMessage saved = savedDmMock(UUID.randomUUID(), Instant.now(), toSave);
            return saved;
        }).given(directMessageRepository).save(dmCaptor.capture());

        given(directMessageMapper.toDto(any(DirectMessage.class))).willReturn(dtoToReturn);
    }


    private static User stubUser(UUID id, boolean locked) {
        User user = mock(User.class);
        if (id != null) lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.isLocked()).thenReturn(locked);
        return user;
    }

    private static DirectMessage dm(UUID id, Instant createdAt) {
        DirectMessage m = mock(DirectMessage.class);
        lenient().when(m.getId()).thenReturn(id);
        lenient().when(m.getCreatedAt()).thenReturn(createdAt);
        return m;
    }

    private static MessageRequest firstPageRequest(UUID partnerId, int limit) {
        return new MessageRequest(partnerId, null, null, limit);
    }

    private static MessageRequest nextPageRequest(UUID partnerId, Instant cursor, UUID idAfter, int limit) {
        return new MessageRequest(partnerId, cursor, idAfter, limit);
    }

    /*
        테스트
     */

    @Test
    void dm_sender_확인한다_실패() throws Exception {
        // given
        loginAsUserId(myId, myEmail);
        given(userRepository.findById(myId)).willReturn(Optional.empty());

        var request = firstPageRequest(otherId, 10);

        // when n then
        assertThatThrownBy(() -> directMessageService.getMessages(request))
            .isInstanceOf(OtbooException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_FOLLOW_REQUEST);

        then(directMessageRepository).shouldHaveNoInteractions();
    }

    @Test
    void sender_locked_확인한다_실패() throws Exception {
        // given
        loginAsUserId(myId, myEmail);
        User meLocked = stubUser(myId, true);
        given(userRepository.findById(myId)).willReturn(Optional.of(meLocked));

        var request = firstPageRequest(otherId, 10);

        // when n then
        assertThatThrownBy(() -> directMessageService.getMessages(request))
            .isInstanceOf(OtbooException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_LOCKED);

        then(directMessageRepository).shouldHaveNoInteractions();
    }

    @Test
    void dm_receiver_확인한다_실패() throws Exception {
        // given
        loginAsUserId(myId, myEmail);
        User me = stubUser(myId, false);
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userRepository.findById(otherId)).willReturn(Optional.empty());

        var request = firstPageRequest(otherId, 10);

        // when n then
        assertThatThrownBy(() -> directMessageService.getMessages(request))
            .isInstanceOf(OtbooException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_FOLLOW_REQUEST);

        then(directMessageRepository).shouldHaveNoInteractions();
    }


    @Test
    void receiver_locked_확인한다_실패() throws Exception {
        // given
        loginAsUserId(myId, myEmail);
        User me    = stubUser(myId, false);
        User other = stubUser(otherId, true);
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userRepository.findById(otherId)).willReturn(Optional.of(other));

        var request = firstPageRequest(otherId, 10);

        // when n then
        assertThatThrownBy(() -> directMessageService.getMessages(request))
            .isInstanceOf(OtbooException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_LOCKED);

        then(directMessageRepository).shouldHaveNoInteractions();
    }

    @Test
    void 정상적으로_첫페이지_dm수를_가져온다() throws Exception {
        // given
        loginAsUserId(myId, myEmail);
        User me = stubUser(myId, false);
        User other = stubUser(otherId, false);
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userRepository.findById(otherId)).willReturn(Optional.of(other));

        DirectMessage m1 = dm(UUID.randomUUID(), Instant.parse("2025-09-22T12:00:00Z"));
        DirectMessage m2 = dm(UUID.randomUUID(), Instant.parse("2025-09-22T11:00:00Z"));
        DirectMessage m3 = dm(UUID.randomUUID(), Instant.parse("2025-09-22T10:00:00Z"));

        given(directMessageRepository.findFirstPage(any(), any(), any()))
            .willReturn(List.of(m1, m2, m3));
        given(directMessageRepository.countBetween(myId, otherId)).willReturn(42L);
        given(directMessageMapper.toDto(any())).willReturn(mock(DirectMessageDto.class));

        var request = firstPageRequest(otherId, 2);

        // when
        DirectMessageListResponse resp = directMessageService.getMessages(request);

        // then
        assertThat(resp).isNotNull();
        assertThat(resp.data()).hasSize(2);
        assertThat(resp.hasNext()).isTrue();
        assertThat(resp.totalCount()).isEqualTo(42L);

        then(directMessageRepository).should().findFirstPage(any(), any(), any());
        then(directMessageRepository).should().countBetween(myId, otherId);
        then(directMessageMapper).should(atLeast(2)).toDto(any());
    }

    @Test
    void 정상적으로_전체_dm수를_가져온다() throws Exception {
        // given
        loginAsUserId(myId, myEmail);
        User me    = stubUser(myId, false);
        User other = stubUser(otherId, false);
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userRepository.findById(otherId)).willReturn(Optional.of(other));

        DirectMessage m1 = dm(UUID.randomUUID(), Instant.parse("2025-09-22T09:00:00Z"));
        DirectMessage m2 = dm(UUID.randomUUID(), Instant.parse("2025-09-22T08:00:00Z"));

        given(directMessageRepository.findNextPage(any(), any(), any(), any(), any()))
            .willReturn(List.of(m1, m2));

        given(directMessageRepository.countBetween(myId, otherId)).willReturn(40L);
        given(directMessageMapper.toDto(any())).willReturn(mock(DirectMessageDto.class));

        var request = nextPageRequest(otherId, Instant.parse("2025-09-22T10:00:00Z"), null, 2);

        // when
        DirectMessageListResponse resp = directMessageService.getMessages(request);

        // then
        assertThat(resp).isNotNull();
        assertThat(resp.data()).hasSize(2);
        assertThat(resp.hasNext()).isFalse();
        assertThat(resp.totalCount()).isEqualTo(40L);

        then(directMessageRepository).should().findNextPage(any(), any(), any(), any(), any());
        then(directMessageRepository).should().countBetween(myId, otherId);
        then(directMessageMapper).should(atLeast(2)).toDto(any());
    }

    /*            sendMessage()     */
    @Test
    void 메세지_전송한다() throws Exception {
        // given
        String content = "hello";
        SendDmRequest req = new SendDmRequest(myId, otherId, content);

        User senderRef = stubUser(myId, false);
        User receiverRef = stubUser(otherId, false);

        DirectMessageDto dto = mock(DirectMessageDto.class);
        commonGiven(senderRef, receiverRef, dto);

        // when
        DirectMessageDto result = directMessageService.sendMessage(myId, req);

        // then
        assertThat(result).isSameAs(dto);
        then(directMessageRepository).should().save(any(DirectMessage.class));
        then(directMessageMapper).should().toDto(any(DirectMessage.class));
        then(eventPublisher).should().publishEvent(any(DirectMessageReceivedEvent.class));

    }

    @Test
    void 메세지_저장한다() throws Exception {
        // given
        String content = "content-123";
        SendDmRequest req = new SendDmRequest(myId, otherId, content);

        User senderRef = stubUser(myId, false);
        User receiverRef = stubUser(otherId, false);

        DirectMessageDto dto = mock(DirectMessageDto.class);
        commonGiven(senderRef, receiverRef, dto);

        // when
        directMessageService.sendMessage(myId, req);

        // then
        DirectMessage captured = dmCaptor.getValue();
        assertThat(captured.getSender()).isSameAs(senderRef);
        assertThat(captured.getReceiver()).isSameAs(receiverRef);
        assertThat(captured.getMessage()).isEqualTo(content);

    }


    @Test
    void 알람_호출_메서드_동작한다() throws Exception {
        // given
        String content = "notify-me";
        SendDmRequest req = new SendDmRequest(myId, otherId, content);

        User senderRef = stubUser(myId, false);
        User receiverRef = stubUser(otherId, false);

        DirectMessageDto dto = mock(DirectMessageDto.class);
        commonGiven(senderRef, receiverRef, dto);

        // when
        directMessageService.sendMessage(myId, req);

        // then
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        DirectMessageReceivedEvent ev = eventCaptor.getValue();
        assertThat(ev.senderId()).isEqualTo(myId);
        assertThat(ev.receiverId()).isEqualTo(otherId);
        assertThat(ev.content()).isEqualTo(content);

    }

    @Test
    void 메세지_10자_이상은_자른다() throws Exception {
        // given
        String longContent = "ABCDEFGHIJK";
        SendDmRequest req = new SendDmRequest(myId, otherId, longContent);

        User senderRef = stubUser(myId, false);
        User receiverRef = stubUser(otherId, false);

        DirectMessageDto dto = mock(DirectMessageDto.class);
        commonGiven(senderRef, receiverRef, dto);

        // when
        directMessageService.sendMessage(myId, req);

        // then
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        DirectMessageReceivedEvent ev = eventCaptor.getValue();
        assertThat(ev.content()).isEqualTo(longContent);
    }

    @Test
    void 정상적으로_대화방_목록을_가져온다() throws Exception {
        // given
        loginAsUserId(myId, myEmail);

        List<DirectMessage> mockConversations = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            User sender = stubUser(myId, false);
            User receiver = stubUser(UUID.randomUUID(), false);
            DirectMessage dm = mock(DirectMessage.class);
            when(dm.getSender()).thenReturn(sender);
            when(dm.getReceiver()).thenReturn(receiver);
            mockConversations.add(dm);

            DirectMessageRoomDto mockRoomDto = DirectMessageRoomDto.builder()
                .partner(AuthorDto.builder().userId(receiver.getId()).name("user" + i).build())
                .lastMessage("Last message " + i)
                .lastMessageSentAt(Instant.now().plusSeconds(i))
                .build();
            when(directMessageMapper.toRoomDto(eq(receiver), eq(dm))).thenReturn(mockRoomDto);
        }

        given(directMessageRepository.findConversationsByUserId(myId)).willReturn(mockConversations);

        // when
        DirectMessageRoomListResponse response = directMessageService.getConversationList();

        // then
        assertThat(response).isNotNull();
        assertThat(response.rooms()).hasSize(25);
        then(directMessageRepository).should().findConversationsByUserId(myId);
        then(directMessageMapper).should(times(25)).toRoomDto(any(User.class), any(DirectMessage.class));
    }
}