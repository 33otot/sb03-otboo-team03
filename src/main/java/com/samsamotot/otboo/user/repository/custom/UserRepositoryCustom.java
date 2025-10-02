package com.samsamotot.otboo.user.repository.custom;

import com.samsamotot.otboo.user.dto.UserListRequest;
import com.samsamotot.otboo.user.entity.User;
import org.springframework.data.domain.Slice;

/**
 * 사용자 Repository 커스텀 인터페이스
 * 
 * <p>커서 기반 페이지네이션을 위한 사용자 목록 조회 메서드를 정의합니다.</p>
 */
public interface UserRepositoryCustom {
    
    /**
     * 커서 기반 페이지네이션을 사용하여 사용자 목록을 조회합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 사용자 목록과 페이지네이션 정보를 포함하는 Slice
     */
    Slice<User> findUsersWithCursor(UserListRequest request);
    
    /**
     * 필터링 조건에 맞는 전체 사용자 수를 조회합니다.
     * 
     * @param request 사용자 목록 조회 요청 파라미터
     * @return 전체 사용자 수
     */
    long countUsersWithFilters(UserListRequest request);
}
