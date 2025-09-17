package com.samsamotot.otboo.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E002", "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "E003", "잘못된 타입의 값입니다."),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "E004", "접근이 거부되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E005", "인증이 필요합니다."),

    // 사용자 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "US002", "이미 존재하는 사용자입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "US003", "올바르지 않은 이메일 형식입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "US004", "비밀번호가 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "US005", "비밀번호가 일치하지 않습니다."),
    USER_LOCKED(HttpStatus.LOCKED, "US006", "계정이 잠겨있습니다."),

    // 인증 관련 에러
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AU001", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AU002", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AU003", "리프레시 토큰을 찾을 수 없습니다."),
    CSRF_TOKEN_MISSING(HttpStatus.FORBIDDEN, "AU004", "CSRF 토큰이 누락되었습니다."),

    // 프로필 관련 에러
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PR001", "프로필을 찾을 수 없습니다."),
    INVALID_BIRTH_DATE(HttpStatus.BAD_REQUEST, "PR002", "올바르지 않은 생년월일입니다."),
    INVALID_TEMPERATURE_SENSITIVITY(HttpStatus.BAD_REQUEST, "PR003", "온도 민감도는 1-5 사이의 값이어야 합니다."),

    // 의상 관련 에러
    CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "CL001", "의상을 찾을 수 없습니다."),
    INVALID_CLOTHES_TYPE(HttpStatus.BAD_REQUEST, "CL002", "올바르지 않은 의상 타입입니다."),
    CLOTHES_ATTRIBUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "CL003", "의상 속성을 찾을 수 없습니다."),
    CLOTHES_ATTRIBUTE_DEF_ALREADY_EXISTS(HttpStatus.CONFLICT, "CL006", "이미 존재하는 의상 속성 정의입니다."),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "CL004", "올바르지 않은 이미지 형식입니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CL005", "이미지 업로드에 실패했습니다."),

    // 피드 관련 에러
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "FD001", "피드를 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "FD002", "댓글을 찾을 수 없습니다."),
    FORBIDDEN_FEED_MODIFICATION(HttpStatus.FORBIDDEN, "FD003", "본인이 작성한 피드만 수정할 수 있습니다."),
    FORBIDDEN_FEED_DELETION(HttpStatus.FORBIDDEN, "FD004", "본인이 작성한 피드만 삭제할 수 있습니다."),

    // 팔로우 관련 에러
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FO001", "팔로우 관계를 찾을 수 없습니다."),
    INVALID_FOLLOW_REQUEST(HttpStatus.BAD_REQUEST, "FO002", "팔로우 관계를 생성할 수 없습니다."),


    // 다이렉트 메시지 관련 에러
    DM_NOT_FOUND(HttpStatus.NOT_FOUND, "DM001", "메시지를 찾을 수 없습니다."),

    // 알림 관련 에러
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NO001", "알림을 찾을 수 없습니다."),

    // 날씨 관련 에러
    WEATHER_NOT_FOUND(HttpStatus.NOT_FOUND, "WE001", "날씨 정보를 찾을 수 없습니다."),
    WEATHER_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "WE002", "날씨 API 호출에 실패했습니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "WE003", "올바르지 않은 위치 정보입니다."),

    // 관리자 관련 에러
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AD001", "관리자 권한이 필요합니다."),
    USER_ROLE_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "AD002", "사용자 권한 업데이트에 실패했습니다."),

    // 커서 기반 페이지네이션 관련 에러
    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "P001", "잘못된 커서 형식입니다."),
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST,"P002", "지원하지 않는 정렬 필드입니다."),
    INVALID_SORT_DIRECTION(HttpStatus.BAD_REQUEST,"P003", "지원하지 않는 정렬 방향입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
