package com.samsamotot.otboo.user.controller;

import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.NotificationSettingUpdateRequest;
import com.samsamotot.otboo.profile.service.ProfileService;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.user.controller.api.UserApi;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.dto.UserDtoCursorResponse;
import com.samsamotot.otboo.user.dto.UserListRequest;
import com.samsamotot.otboo.user.dto.UserRoleUpdateRequest;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 사용자 관련 HTTP 요청을 처리하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final String CONTROLLER = "[UserController] ";

    private final UserService userService;
    private final ProfileService profileService;

    /**
     * 신규 사용자 회원가입을 처리합니다.
     *
     * @param request 회원가입 요청 정보를 담은 DTO (email, name, password)
     * @return 생성된 사용자 정보와 201 CREATED 상태코드를 담은 ResponseEntity
     */
    @Override
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info(CONTROLLER + "회원가입 요청 - 이메일: {}", request.getEmail());

        UserDto userDto = userService.createUser(request);

        log.info(CONTROLLER + "회원가입 완료 - 사용자 ID: {}", userDto.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    /**
     * 사용자 ID를 기반으로 특정 사용자의 프로필을 조회합니다.
     *
     * @param userId 조회할 사용자의 고유 ID (UUID)
     * @return 조회된 프로필 정보와 200 OK 상태코드를 담은 ResponseEntity
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
     * @param image 새로 등록할 프로필 이미지 파일 (선택 사항)
     * @return 수정이 완료된 최신 프로필 정보가 담긴 ResponseEntity<ProfileDto>
     * @throws OtbooException 사용자를 찾을 수 없거나 파일 처리 중 오류 발생 시 예외
     */
    @Override
    @PatchMapping(value = "/{userId}/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileDto> updateProfile(
            @PathVariable UUID userId,
            @RequestPart("request") @Valid ProfileUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info(CONTROLLER + "사용자 프로필 수정 요청 - 사용자 ID: {}", userId);

        ProfileDto profileDto = profileService.updateProfile(userId, request, image);

        log.info(CONTROLLER + "사용자 프로필 수정 성공 - 사용자 ID: {}, 프로필: {}", userId, profileDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(profileDto);
    }

    /**
     * 다양한 조건에 따라 사용자 목록을 조회합니다. (커서 기반 페이징)
     *
     * @param limit         한 페이지에 표시할 항목 수
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향 ("ASC" 또는 "DESC")
     * @param cursor        페이지네이션을 위한 커서 값
     * @param idAfter       커서 기반 페이지네이션을 위한 ID
     * @param emailLike     이메일 주소 필터링 (포함 검색)
     * @param roleEqual     사용자 역할 필터링 (정확히 일치)
     * @param locked        계정 잠금 상태 필터링
     * @return 페이징 및 필터링된 사용자 목록과 전체 개수를 담은 ResponseEntity
     */
    @Override
    @GetMapping
    public ResponseEntity<UserDtoCursorResponse> getUserList(
        @RequestParam int limit,
        @RequestParam String sortBy,
        @RequestParam String sortDirection,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(required = false) String emailLike,
        @RequestParam(required = false) String roleEqual,
        @RequestParam(required = false) Boolean locked
    ) {
        log.info(CONTROLLER + "사용자 목록 조회 요청 - limit: {}, sortBy: {}, sortDirection: {}",
            limit, sortBy, sortDirection);

        // Role enum 변환
        Role role = null;
        if (roleEqual != null && !roleEqual.isEmpty()) {
            try {
                role = Role.valueOf(roleEqual.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn(CONTROLLER + "잘못된 권한 값: {}", roleEqual);
                // 잘못된 권한 값은 무시하고 계속 진행
            }
        }

        // UserListRequest 생성
        UserListRequest request = UserListRequest.builder()
            .limit(limit)
            .sortBy(sortBy)
            .sortDirection(sortDirection)
            .cursor(cursor)
            .idAfter(idAfter)
            .emailLike(emailLike)
            .roleEqual(role)
            .locked(locked)
            .build();

        // 사용자 목록 조회
        UserDtoCursorResponse response = userService.getUserList(request);

        log.info(CONTROLLER + "사용자 목록 조회 완료 - 조회된 수: {}, 전체 수: {}",
            response.data().size(), response.totalCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 역할을 수정합니다. (ADMIN 권한 필요)
     *
     * @param userId  역할을 수정할 사용자의 UUID
     * @param request 새로운 역할을 담은 요청 DTO
     * @return 수정된 사용자 정보와 200 OK 상태코드를 담은 ResponseEntity. ADMIN이 아닐 경우 403 Forbidden.
     */
    @Override
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDto> updateRole(
        @PathVariable UUID userId,
        @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        log.info(CONTROLLER + "권한 수정 요청 - 사용자 ID: {}, 새로운 권한: {}", userId, request.role());

        // ADMIN 권한 검증
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            log.warn(CONTROLLER + "권한 수정 실패 - 인증 정보 없음");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (!"ADMIN".equals(userDetails.getRole())) {
            log.warn(CONTROLLER + "권한 수정 실패 - ADMIN 권한 필요, 현재 권한: {}", userDetails.getRole());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserDto result = userService.updateUserRole(userId, request);

        log.info(CONTROLLER + "권한 수정 완료 - 사용자 ID: {}, 변경된 권한: {}", userId, result.getRole());

        return ResponseEntity.ok(result);
    }

    /**
     * 현재 인증된 사용자의 날씨 알림 수신 여부를 수정합니다.
     *
     * @param userDetails 현재 인증된 사용자의 상세 정보
     * @param request     날씨 알림 설정 값을 담은 요청 DTO
     * @return 작업 성공 시 200 OK 상태코드를 담은 ResponseEntity
     */
    @PatchMapping("/profiles/notification-weathers")
    public ResponseEntity<Void> updateWeatherNotificationEnabled(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSettingUpdateRequest request
    ) {
        log.info(CONTROLLER + "프로필 알림 수신 여부 수정 요청 - NotificationEnabled: {}", request.weatherNotificationEnabled());

        profileService.updateNotificationEnabled(userDetails.getId(), request);

        log.info(CONTROLLER + "프로필 알림 수신 여부 수정 완료");

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
