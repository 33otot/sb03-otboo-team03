package com.samsamotot.otboo.user.controller;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.user.controller.api.UserApi;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userDto);
    }

    /**
     * 사용자 ID를 기반으로 특정 사용자의 프로필을 조회합니다.
     *
     * @param userId 조회할 사용자의 고유 ID (UUID)
     * @return 사용자 프로필 정보가 담긴 ResponseEntity<ProfileDto>
     * @throws OtbooException 사용자를 찾을 수 없을 때 발생하는 예외
     */
    @Override
    @GetMapping("/{userId}/profiles")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID userId) {
        log.info(CONTROLLER + "사용자 프로필 조회 요청 - 사용자 ID: {}", userId);

        ProfileDto profileDto = profileService.getProfileByUserId(userId);

        log.info(CONTROLLER + "사용자 프로필 조회 완료");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(profileDto);
    }

    /**
     * 특정 사용자의 프로필 정보와 프로필 이미지를 수정합니다.
     * 이 메서드는 multipart/form-data 형식의 요청을 처리하며,
     * JSON 데이터와 이미지 파일을 각각의 파트로 받아 처리합니다.
     *
     * @param userId       수정할 사용자의 고유 ID (UUID)
     * @param request      수정할 프로필 정보가 담긴 DTO (@Valid를 통해 유효성 검사)
     * @param profileImage 새로 등록할 프로필 이미지 파일 (선택 사항)
     * @return 수정이 완료된 최신 프로필 정보가 담긴 ResponseEntity<ProfileDto>
     * @throws OtbooException 사용자를 찾을 수 없거나 파일 처리 중 오류 발생 시 예외
     */
    @Override
    @PatchMapping(value = "/{userId}/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileDto> updateProfile(
            @PathVariable UUID userId,
            @RequestPart("request") @Valid ProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false)MultipartFile profileImage
    ) {
        log.info(CONTROLLER + "사용자 프로필 수정 요청 - 사용자 ID: {}", userId);

        ProfileDto profileDto = profileService.updateProfile(userId, request, profileImage);

        log.info(CONTROLLER + "사용지 프로필 수정 성공 - 사용자 ID: {}, 프로필: {}", userId, profileDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(profileDto);
    }
}
