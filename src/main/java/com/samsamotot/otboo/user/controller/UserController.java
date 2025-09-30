package com.samsamotot.otboo.user.controller;

import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.controller.api.UserApi;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 사용자 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final String CONTROLLER = "[UserController] ";

    private final UserService userService;
    private final ProfileService profileService;

    @Override
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info(CONTROLLER + "회원가입 요청 - 이메일: {}", request.getEmail());

        UserDto userDto = userService.createUser(request);

        log.info(CONTROLLER + "회원가입 완료 - 사용자 ID: {}", userDto.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @Override
    @GetMapping("/{userId}/profiles")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID userId) {
        log.info(CONTROLLER + "사용자 프로필 조회 요청 - 사용자 ID: {}", userId);

        ProfileDto profileDto = profileService.getProfileByUserId(userId);

        log.info(CONTROLLER + "사용자 프로필 조회 완료");

        return ResponseEntity.status(HttpStatus.OK).body(profileDto);
    }

}
