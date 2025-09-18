package com.samsamotot.otboo.user.controller;

import com.samsamotot.otboo.user.controller.api.UserApi;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
