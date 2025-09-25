package com.samsamotot.otboo.common.util;

import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    private AuthUtil() {
        throw new AssertionError("AuthUtil 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * SecurityContextHolder에서 현재 인증된 사용자의 ID를 가져옵니다.
     *
     * @return 현재 사용자의 UUID
     * @throws AuthenticationCredentialsNotFoundException 인증 정보가 없거나, 사용자 정보가 잘못된 경우
     */
    public static UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            UUID id = userDetails.getId();
            if (id != null) {
                return id;
            }
            throw new AuthenticationCredentialsNotFoundException("사용자 ID가 존재하지 않습니다.");
        }

        String principalType = principal != null ? principal.getClass().getName() : "null";

        throw new AuthenticationCredentialsNotFoundException("유효한 사용자 정보가 아닙니다. Principal type: " + principalType);
    }
}
