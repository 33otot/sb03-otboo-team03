package com.samsamotot.otboo.directmessage.controller;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.controller
 * FileName     : DirectMessageController
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("api/direct-messages")
public class DirectMessageController {
    private final DirectMessageService directMessageService;


    @GetMapping
    public ResponseEntity<CursorResponse<DirectMessage>> directMessages(
        @RequestParam UUID userId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit
    ) {
        MessageRequest request = new MessageRequest(userId, cursor, idAfter, limit);
        return ResponseEntity.ok().body(directMessageService.getMessages(request));
    }
}
