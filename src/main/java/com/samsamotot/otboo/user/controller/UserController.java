package com.samsamotot.otboo.user.controller;

import com.samsamotot.otboo.user.controller.api.UserApi;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.dto.UserDtoCursorResponse;
import com.samsamotot.otboo.user.dto.UserListRequest;
import com.samsamotot.otboo.user.dto.UserRoleUpdateRequest;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.service.UserService;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    
    @Override
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info(CONTROLLER + "회원가입 요청 - 이메일: {}", request.getEmail());
        
        UserDto userDto = userService.createUser(request);
        
        log.info(CONTROLLER + "회원가입 완료 - 사용자 ID: {}", userDto.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }
    
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
}
