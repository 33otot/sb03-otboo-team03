package com.samsamotot.otboo.directmessage.mapper;

import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * PackageName  : com.samsamotot.otboo.directmessage
 * FileName     : DirectMessageMapper
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface DirectMessageMapper {

    @Mapping(source = "sender", target = "sender")
    @Mapping(source = "receiver", target = "receiver")
    @Mapping(source = "message", target = "content")
    @Mapping(source = "createdAt",   target = "createdAt")
    DirectMessageDto toDto(DirectMessage entity);
}


