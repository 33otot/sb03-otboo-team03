package com.samsamotot.otboo.user.dto;

import com.samsamotot.otboo.user.entity.Role;
import lombok.Builder;

import java.util.UUID;

/**
 * 사용자 목록 조회 요청 DTO
 * 
 * <p>계정 목록 조회 API의 요청 파라미터를 위한 데이터 전송 객체입니다.
 * 커서 기반 페이지네이션과 다양한 필터링 옵션을 지원합니다.</p>
 * 
 * <h3>주요 필드:</h3>
 * <ul>
 *   <li><strong>limit</strong>: 페이지 크기 (필수)</li>
 *   <li><strong>sortBy</strong>: 정렬 기준 (email | createdAt)</li>
 *   <li><strong>sortDirection</strong>: 정렬 방향 (ASCENDING | DESCENDING)</li>
 *   <li><strong>cursor</strong>: 페이지네이션 커서 (선택)</li>
 *   <li><strong>idAfter</strong>: 보조 커서 (선택)</li>
 *   <li><strong>emailLike</strong>: 이메일 검색 (선택)</li>
 *   <li><strong>roleEqual</strong>: 권한 필터 (선택)</li>
 *   <li><strong>locked</strong>: 잠금 상태 필터 (선택)</li>
 * </ul>
 */
@Builder
public record UserListRequest(
    int limit,
    String sortBy,
    String sortDirection,
    String cursor,
    UUID idAfter,
    String emailLike,
    Role roleEqual,
    Boolean locked
) {
    
    /**
     * 기본값으로 UserListRequest를 생성하는 정적 팩토리 메서드
     * 
     * @param limit 페이지 크기
     * @return 기본 설정이 적용된 UserListRequest
     */
    public static UserListRequest of(int limit) {
        return UserListRequest.builder()
            .limit(limit)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .build();
    }
    
    /**
     * 이메일 검색이 포함된 UserListRequest를 생성하는 정적 팩토리 메서드
     * 
     * @param limit 페이지 크기
     * @param emailLike 검색할 이메일 패턴
     * @return 이메일 검색이 포함된 UserListRequest
     */
    public static UserListRequest withEmailSearch(int limit, String emailLike) {
        return UserListRequest.builder()
            .limit(limit)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .emailLike(emailLike)
            .build();
    }
    
    /**
     * 권한별 필터링이 포함된 UserListRequest를 생성하는 정적 팩토리 메서드
     * 
     * @param limit 페이지 크기
     * @param roleEqual 필터링할 권한
     * @return 권한별 필터링이 포함된 UserListRequest
     */
    public static UserListRequest withRoleFilter(int limit, Role roleEqual) {
        return UserListRequest.builder()
            .limit(limit)
            .sortBy("createdAt")
            .sortDirection("DESCENDING")
            .roleEqual(roleEqual)
            .build();
    }
}
