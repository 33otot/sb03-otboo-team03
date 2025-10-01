package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.service
 * FileName     : directMessageService
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Validated
public interface DirectMessageService {
    DirectMessageListResponse getMessages(@Valid MessageRequest request);
}
