package com.samsamotot.otboo.directmessage.controller;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.*;
import com.samsamotot.otboo.directmessage.controller.api.DirectMessageApi;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

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

    // TODO 작업중
    @MessageMapping("/direct-messages_send")
    public void send(SendDmRequest request, Principal principal) {
        if (principal == null) {
            log.error("Unauthorized WebSocket request");
            return;
        }
        
        UUID me = UUID.fromString(principal.getName());
        log.info(DM_CONTROLLER + "incoming: from={} to={} content={}", 
            me, request.receiverId(), request.content());

        DmEvent event = directMessageService.persistAndBuildEvent(me, request);

        String destination = DmTopicKey.destination(event.senderId(), event.receiverId());
        template.convertAndSend(destination, event);
        
        log.info(DM_CONTROLLER + "broadcast -> {} id={}", destination, event.id());
    }
}
