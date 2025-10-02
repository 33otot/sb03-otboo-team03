package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.mapper.DirectMessageMapper;
import com.samsamotot.otboo.directmessage.repository.DirectMessageRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
    private UserRepository userRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private DirectMessageMapper directMessageMapper;

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


    private static MessageRequest newRequest(UUID partnerId, int limit) {
        return new MessageRequest(partnerId, null, null, limit);
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
    void receiver_locked_확인한다_실패() {
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
    void 정상적으로_첫페이지_dm수를_가져온다() {
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

    /*
            sendMessage()
     */
    @Test
    void 메세지_전송한다() throws Exception {
        // given

        // when

        // then

    }
    @Test
    void 메세지_저장한다() throws Exception {
        // given

        // when

        // then

    }
    @Test
    void 알람_호출_메서드_동작한다() throws Exception {
        // given

        // when

        // then

    }

    @Test
    void 메세지_10자_이상은_자른다() throws Exception {
        // given

        // when

        // then

    }
}