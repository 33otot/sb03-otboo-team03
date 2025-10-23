package com.samsamotot.otboo.common.security.csrf;

import com.samsamotot.otboo.common.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.samsamotot.otboo.common.config.SecurityTestConfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = CsrfController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { JwtAuthenticationFilter.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityTestConfig.class)
class CsrfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CsrfTokenRepository csrfTokenRepository;

    @Test
    @DisplayName("GET /api/auth/csrf-token: 204, 저장소에 토큰 저장 호출")
    void getCsrfToken_success() throws Exception {
        CsrfToken mockToken = Mockito.mock(CsrfToken.class);
        given(mockToken.getHeaderName()).willReturn("X-XSRF-TOKEN");
        given(mockToken.getParameterName()).willReturn("_csrf");
        given(mockToken.getToken()).willReturn("test-token");
        given(csrfTokenRepository.generateToken(any(HttpServletRequest.class))).willReturn(mockToken);

        mockMvc.perform(get("/api/auth/csrf-token"))
            .andExpect(status().isNoContent());

        verify(csrfTokenRepository, times(1))
            .saveToken(eq(mockToken), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}


