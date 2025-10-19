package com.samsamotot.otboo.directmessage.mapper;

import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomDto;
import com.samsamotot.otboo.directmessage.entity.DirectMessage;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.mapper.UserMapper;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
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

    @Mapping(source = "sender", target = "sender", qualifiedByName = "toAuthorDto")
    @Mapping(source = "receiver", target = "receiver", qualifiedByName = "toAuthorDto")
    @Mapping(source = "message", target = "content")
    @Mapping(source = "createdAt",   target = "createdAt")
    DirectMessageDto toDto(DirectMessage entity);

    @Mapping(target = "partner", expression = "java(userMapper.toAuthorDto(partner, profileUrlMap))")
    @Mapping(source = "dm.message", target = "lastMessage")
    @Mapping(source = "dm.createdAt", target = "lastMessageSentAt")
    DirectMessageRoomDto toRoomDto(
        User partner,
        DirectMessage dm,
        @Context Map<UUID, String> profileUrlMap
    );
}


