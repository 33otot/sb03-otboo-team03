package com.samsamotot.otboo.common.util;

import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

public class AuthUtil {

    private AuthUtil() {
        throw new AssertionError("AuthUtil 클래스는 인스턴스화할 수 없습니다.");
    }

    public static UUID getAuthenticatedUserId(CustomUserDetails principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return principal.getId();
    }
}
