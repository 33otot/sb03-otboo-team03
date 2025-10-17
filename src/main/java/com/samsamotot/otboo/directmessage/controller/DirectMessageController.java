package com.samsamotot.otboo.directmessage.controller;

import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.directmessage.controller.api.DirectMessageApi;
import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomListResponse;
import com.samsamotot.otboo.directmessage.dto.DmTopicKey;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.dto.SendDmRequest;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.controller
 * FileName     : DirectMessageController
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */

@Slf4j
@RequiredArgsConstructor
@Controller
@Validated
@RequestMapping("api/direct-messages")
public class DirectMessageController implements DirectMessageApi {
    private static final String DM_CONTROLLER = "[DirectMessageController]";

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate template;

    /**
     * DM 목록을 조회하는 API
     *
     * @param userId   상대방 사용자 ID
     * @param cursor   커서 기반 페이지네이션을 위한 기준 시각(선택)
     * @param idAfter  동일 시각일 경우 이어지는 메시지 기준 ID(선택)
     * @param limit    조회할 메시지 개수
     * @return         메시지 목록 및 페이지네이션 정보
     */
    @GetMapping
    public ResponseEntity<DirectMessageListResponse> directMessages(
        @RequestParam UUID userId,
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit
    ) {
        MessageRequest request = new MessageRequest(userId, cursor, idAfter, limit);
        return ResponseEntity.ok().body(directMessageService.getMessages(request));
    }

    /**
     * 현재 사용자의 모든 대화방 목록을 조회하는 API
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 대화방 목록 및 마지막 메시지 정보
     */
    @GetMapping("/rooms")
    public ResponseEntity<DirectMessageRoomListResponse> getConversationRooms(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok().body(directMessageService.getConversationList());
    }

    /**
     * WebSocket 클라이언트가 "/direct-messages_send" 경로로 보낸 메시지를 처리한다.
     *
     * <p>흐름:
     * 1. 인증 Principal을 확인한다 (없으면 null 반환).
     * 2. 요청을 서비스 계층에 위임해 메시지를 저장하고 DTO를 얻는다.
     * 3. 대화 상대 두 명(me, receiverId)에 대한 STOMP destination을 계산한다.
     * 4. SimpMessagingTemplate을 이용해 해당 destination으로 브로드캐스트한다.
     *
     * @param request   전송 요청 (수신자 ID, 내용 등)
     * @param principal 인증 정보 (현재 WebSocket 세션 사용자)
     * @return 저장 후 브로드캐스트된 DirectMessage DTO (인증 없으면 null)
     */
    @MessageMapping("/direct-messages_send")
    public DirectMessageDto send(@Valid SendDmRequest request, Principal principal) {
        if (principal == null) {
            log.error(DM_CONTROLLER + " Unauthorized WebSocket request");
            return null;
        }
        
        UUID me = UUID.fromString(principal.getName());
        log.info(DM_CONTROLLER + " incoming: from={} to={} content={}",
            me, request.receiverId(), request.content());

        DirectMessageDto response = directMessageService.sendMessage(me, request);

        String destination = DmTopicKey.destination(me, request.receiverId());
        template.convertAndSend(destination, response);

        return response;
    }
}
