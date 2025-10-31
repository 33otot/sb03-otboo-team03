package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomCursorRequest;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.dto.SendDmRequest;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomListResponse; // Added
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : directMessageService
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Validated
public interface DirectMessageService {
    DirectMessageListResponse getMessages(@Valid MessageRequest request);

    DirectMessageDto sendMessage(UUID senderId, SendDmRequest request);

    DirectMessageRoomListResponse getConversationList(DirectMessageRoomCursorRequest request);
}
