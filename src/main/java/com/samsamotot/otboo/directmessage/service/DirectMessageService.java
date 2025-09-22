package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import org.springframework.stereotype.Service;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : directMessageService
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Service
public interface DirectMessageService {
    CursorResponse<DirectMessage> getMessages(MessageRequest request);
}
