package com.samsamotot.otboo.directmessage.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.directmessage.dto.*;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
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
    // TODO 작업중
    DmEvent persistAndBuildEvent(UUID senderId, SendDmRequest request);
}
