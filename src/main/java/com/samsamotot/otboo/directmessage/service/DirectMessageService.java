package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.*;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : directMessageService
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Service
public interface DirectMessageService {
    DirectMessageListResponse getMessages(MessageRequest request);

    DmEvent persistAndBuildEvent(UUID senderId, SendDmRequest request);

    DmReadEvent markRead(UUID me, DmReadRequest request);
}
