package com.samsamotot.otboo.user.config;

import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
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

    private final String ADMIN_INITIALIZER = "[AdminInitializer] ";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;

    /**
     * 관리자 계정 초기화 설정(application.yaml:.env파일 참조)
     */
    @Value("${app.admin.email}")
    private String adminEmail;

    /**
     * 관리자 계정 초기화 설정(application.yaml:.env파일 참조)
     */
    @Value("${app.admin.username}")
    private String adminUsername;

    /**
     * 관리자 계정 초기화 설정(application.yaml:.env파일 참조)
     */
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
            log.info(ADMIN_INITIALIZER + "관리자 계정 생성을 생략합니다.");
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

        Profile adminProfile = Profile.builder()
                .user(adminUser)
                .name(adminUser.getUsername())
                .weatherNotificationEnabled(true)
                .build();

        profileRepository.save(adminProfile);

        log.info(ADMIN_INITIALIZER + "관리자 계정({})이 성공적으로 생성되었습니다.", adminUsername);

        } catch (Exception e) {
        log.error(ADMIN_INITIALIZER + "관리자 계정 생성 중 오류가 발생했습니다.", e);
        }
    }
}
