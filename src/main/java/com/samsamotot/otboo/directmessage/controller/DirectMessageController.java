package com.samsamotot.otboo.directmessage.controller;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.*;
import com.samsamotot.otboo.directmessage.controller.api.DirectMessageApi;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.security.Principal;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.controller
 * FileName     : DirectMessageController
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("api/direct-messages")
public class DirectMessageController implements DirectMessageApi {

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

    @MessageMapping("/direct-messages_send")
    public void send(SendDmRequest req, Principal principal) {
        UUID me = UUID.fromString(principal.getName());
        var event = directMessageService.persistAndBuildEvent(me, req);

        String topic = DmTopicKey.topic(event.senderId(), event.receiverId());
        template.convertAndSend(topic, event);
    }

//    @MessageMapping("/dm.read")
//    public void read(DmReadRequest req, Principal principal) {
//        UUID me = UUID.fromString(principal.getName());
//        DmReadEvent ev = directMessageService.markRead(me, req);
//
//        // 상대에게: 내가 읽었다는 신호
//        template.convertAndSendToUser(req.peerId().toString(), "/queue/dm.read", ev);
//
//        // 나에게도(동기화용)
//        template.convertAndSendToUser(me.toString(), "/queue/dm.read", ev);
//    }
}
