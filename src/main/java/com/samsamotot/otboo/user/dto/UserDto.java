package com.samsamotot.otboo.user.dto;

import com.samsamotot.otboo.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private Instant createdAt;
    private String email;
    private String name;
    private Role role;
    private List<String> linkedOAuthProviders;
    private Boolean locked;
}
