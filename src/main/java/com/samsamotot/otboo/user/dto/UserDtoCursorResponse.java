package com.samsamotot.otboo.user.dto;

import com.samsamotot.otboo.common.dto.CursorResponse;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 목록 조회 응답 DTO
 * 
 * <p>계정 목록 조회 API의 응답을 위한 데이터 전송 객체입니다.
 * 커서 기반 페이지네이션을 지원하며, 사용자 목록과 페이지네이션 정보를 포함합니다.</p>
 * 
 * <h3>주요 필드:</h3>
 * <ul>
 *   <li><strong>data</strong>: 사용자 목록 (UserDto 배열)</li>
 *   <li><strong>nextCursor</strong>: 다음 페이지 조회를 위한 커서</li>
 *   <li><strong>nextIdAfter</strong>: 다음 페이지 조회를 위한 보조 커서 (UUID)</li>
 *   <li><strong>hasNext</strong>: 다음 페이지 존재 여부</li>
 *   <li><strong>totalCount</strong>: 전체 사용자 수</li>
 *   <li><strong>sortBy</strong>: 정렬 기준 필드</li>
 *   <li><strong>sortDirection</strong>: 정렬 방향 (ASCENDING | DESCENDING)</li>
 * </ul>
 */
@Builder
public record UserDtoCursorResponse(
    List<UserDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
    
    /**
     * CursorResponse를 UserDtoCursorResponse로 변환하는 정적 팩토리 메서드
     * 
     * @param cursorResponse 변환할 CursorResponse 객체
     * @return UserDtoCursorResponse 객체
     */
    public static UserDtoCursorResponse from(CursorResponse<UserDto> cursorResponse) {
        return UserDtoCursorResponse.builder()
            .data(cursorResponse.data())
            .nextCursor(cursorResponse.nextCursor())
            .nextIdAfter(cursorResponse.nextIdAfter())
            .hasNext(cursorResponse.hasNext())
            .totalCount(cursorResponse.totalCount())
            .sortBy(cursorResponse.sortBy())
            .sortDirection(cursorResponse.sortDirection().name())
            .build();
    }
}
