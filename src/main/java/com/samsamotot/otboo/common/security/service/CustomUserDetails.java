package com.samsamotot.otboo.common.security.service;

import com.samsamotot.otboo.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security UserDetails 인터페이스의 커스텀 구현체
 * 
 * <p>이 클래스는 애플리케이션의 User 엔티티를 Spring Security의 UserDetails로
 * 변환하여 인증 및 권한 관리를 가능하게 합니다. 사용자 정보, 권한, 계정 상태 등을
 * Spring Security가 이해할 수 있는 형태로 제공합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>사용자 정보 변환</strong>: User 엔티티를 Spring Security UserDetails로 변환</li>
 *   <li><strong>권한 관리</strong>: 사용자 역할을 Spring Security 권한으로 변환</li>
 *   <li><strong>계정 상태 관리</strong>: 계정 잠금, 만료, 활성화 상태 관리</li>
 *   <li><strong>인증 정보 제공</strong>: Spring Security가 인증에 필요한 모든 정보 제공</li>
 * </ul>
 * 
 * <h3>Spring Security 통합:</h3>
 * <ul>
 *   <li><strong>getUsername()</strong>: 사용자 이메일을 사용자명으로 사용</li>
 *   <li><strong>getAuthorities()</strong>: 사용자 역할을 "ROLE_" 접두사와 함께 권한으로 변환</li>
 *   <li><strong>isAccountNonLocked()</strong>: 계정 잠금 상태 확인</li>
 *   <li><strong>isEnabled()</strong>: 계정 활성화 상태 확인</li>
 * </ul>
 * 
 * <h3>권한 매핑:</h3>
 * <ul>
 *   <li><strong>USER</strong> → "ROLE_USER"</li>
 *   <li><strong>ADMIN</strong> → "ROLE_ADMIN"</li>
 * </ul>
 * 
 * <h3>계정 상태 관리:</h3>
 * <ul>
 *   <li><strong>isAccountNonExpired()</strong>: 항상 true (계정 만료 없음)</li>
 *   <li><strong>isAccountNonLocked()</strong>: User.isLocked()의 반대값</li>
 *   <li><strong>isCredentialsNonExpired()</strong>: 항상 true (자격 증명 만료 없음)</li>
 *   <li><strong>isEnabled()</strong>: 항상 true (계정 활성화)</li>
 * </ul>
 */
@Getter
public class CustomUserDetails implements UserDetails {
    
    private final UUID id;
    private final String email;
    private final String password;
    private final String role;
    private final boolean locked;
    
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole().name();
        this.locked = user.isLocked();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
