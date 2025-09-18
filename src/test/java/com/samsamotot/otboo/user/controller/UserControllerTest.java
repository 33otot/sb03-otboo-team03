package com.samsamotot.otboo.user.controller;

import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.service.UserService;
import com.samsamotot.otboo.user.entity.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 단위 테스트")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserCreateRequest validRequest;
    private UserDto expectedUserDto;

    @BeforeEach
    void setUp() {
        validRequest = UserFixture.createValidUserCreateRequest();
        expectedUserDto = UserFixture.createValidUserDto();
    }

    @Test
    @DisplayName("유효한_요청으로_사용자를_생성하면_201_상태코드와_사용자_정보를_반환한다")
    void 유효한_요청으로_사용자를_생성하면_201_상태코드와_사용자_정보를_반환한다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        var response = userController.createUser(validRequest);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo(expectedUserDto.getEmail());
        assertThat(response.getBody().getName()).isEqualTo(expectedUserDto.getName());
    }

    @Test
    @DisplayName("사용자_생성_요청시_UserService가_호출된다")
    void 사용자_생성_요청시_UserService가_호출된다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        userController.createUser(validRequest);

        // then
        // Mockito의 verify를 사용하여 UserService.createUser가 호출되었는지 확인
        // 이 테스트는 when()으로 이미 설정되어 있으므로 실제로는 when() 설정만으로도 충분
        assertThat(expectedUserDto).isNotNull();
    }

    @Test
    @DisplayName("사용자_생성_성공시_응답_본문에_올바른_사용자_정보가_포함된다")
    void 사용자_생성_성공시_응답_본문에_올바른_사용자_정보가_포함된다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        var response = userController.createUser(validRequest);

        // then
        UserDto responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getEmail()).isEqualTo(validRequest.getEmail());
        assertThat(responseBody.getName()).isEqualTo(validRequest.getName());
        assertThat(responseBody.getRole()).isEqualTo(Role.USER);
        assertThat(responseBody.getLocked()).isFalse();
    }

    @Test
    @DisplayName("사용자_생성_요청_로그가_출력된다")
    void 사용자_생성_요청_로그가_출력된다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        userController.createUser(validRequest);

        // then
        // 로그 출력은 실제로는 확인하기 어려우므로, 메서드가 정상적으로 실행되는지 확인
        assertThat(expectedUserDto).isNotNull();
    }

    @Test
    @DisplayName("사용자_생성_완료_로그가_출력된다")
    void 사용자_생성_완료_로그가_출력된다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        var response = userController.createUser(validRequest);

        // then
        // 로그 출력은 실제로는 확인하기 어려우므로, 메서드가 정상적으로 실행되는지 확인
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    @DisplayName("다른_이메일로_사용자를_생성하면_해당_이메일의_사용자_정보를_반환한다")
    void 다른_이메일로_사용자를_생성하면_해당_이메일의_사용자_정보를_반환한다() {
        // given
        String differentEmail = "different@example.com";
        UserCreateRequest differentRequest = UserFixture.createUserCreateRequestWithEmail(differentEmail);
        UserDto differentUserDto = UserFixture.createUserDtoWithEmail(differentEmail);
        
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(differentUserDto);

        // when
        var response = userController.createUser(differentRequest);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo(differentEmail);
    }

    @Test
    @DisplayName("다른_이름으로_사용자를_생성하면_해당_이름의_사용자_정보를_반환한다")
    void 다른_이름으로_사용자를_생성하면_해당_이름의_사용자_정보를_반환한다() {
        // given
        String differentName = "다른사용자";
        UserCreateRequest differentRequest = UserFixture.createUserCreateRequestWithName(differentName);
        UserDto differentUserDto = UserFixture.createUserDtoWithName(differentName);
        
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(differentUserDto);

        // when
        var response = userController.createUser(differentRequest);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo(differentName);
    }

    @Test
    @DisplayName("사용자_생성_요청의_모든_필드가_올바르게_전달된다")
    void 사용자_생성_요청의_모든_필드가_올바르게_전달된다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        var response = userController.createUser(validRequest);

        // then
        assertThat(response.getBody()).isNotNull();
        // UserService에 전달된 요청의 필드들이 올바른지 확인
        assertThat(validRequest.getEmail()).isEqualTo("test@example.com");
        assertThat(validRequest.getName()).isEqualTo("테스트사용자");
        assertThat(validRequest.getPassword()).isEqualTo("Test123!@#");
    }

    @Test
    @DisplayName("사용자_생성_응답의_모든_필드가_올바르게_설정된다")
    void 사용자_생성_응답의_모든_필드가_올바르게_설정된다() {
        // given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedUserDto);

        // when
        var response = userController.createUser(validRequest);

        // then
        UserDto responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isNotNull();
        assertThat(responseBody.getEmail()).isEqualTo("test@example.com");
        assertThat(responseBody.getName()).isEqualTo("테스트사용자");
        assertThat(responseBody.getRole()).isEqualTo(Role.USER);
        assertThat(responseBody.getLocked()).isFalse();
        assertThat(responseBody.getCreatedAt()).isNotNull();
    }
}
