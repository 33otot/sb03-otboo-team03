package com.samsamotot.otboo.user.config;

import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 관리자 계정을 자동으로 생성하는 초기화 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }

    /**
     * 관리자 계정을 초기화합니다.
     * 이미 관리자 계정이 존재하는 경우 생성하지 않습니다.
     */
    private void initializeAdminUser() {
        try {
        // 이미 관리자 계정이 존재하는지 확인
        boolean adminExists = userRepository.findByRole(Role.ADMIN).stream()
            .anyMatch(user -> user.getEmail().equals(adminEmail));

        if (adminExists) {
            log.info("[AdminInitializer] 관리자 계정 생성을 생략합니다.");
            return;
        }

        // 관리자 계정 생성
        User adminUser = User.createAdminUser(
            adminEmail,
            adminUsername,
            adminPassword,
            passwordEncoder
        );

        userRepository.save(adminUser);

        log.info("[AdminInitializer] 관리자 계정이 성공적으로 생성되었습니다.");
        // log.info("[AdminInitializer] 이메일: {}", adminEmail);
        // log.info("[AdminInitializer] 사용자명: {}", adminUsername);
        // log.info("[AdminInitializer] 권한: {}", Role.ADMIN);

        } catch (Exception e) {
        log.error("[AdminInitializer] 관리자 계정 생성 중 오류가 발생했습니다.", e);
        }
    }
}
