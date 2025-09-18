package com.samsamotot.otboo.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 사용자 생성 요청 DTO
 * 
 * <h3>보안 고려사항</h3>
 * <p>이 DTO는 민감한 정보인 비밀번호를 포함하고 있어 다음과 같은 보안 조치를 적용했습니다:</p>
 * <ul>
 *   <li><strong>toString() 제외</strong>: {@code @ToString.Exclude} 어노테이션을 통해 
 *       Lombok이 생성하는 toString() 메서드에서 비밀번호 필드가 제외됩니다. 
 *       이는 로그 출력 시 비밀번호가 노출되는 것을 방지합니다.</li>
 *   <li><strong>JSON 직렬화 제한</strong>: {@code @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)} 
 *       어노테이션을 통해 Jackson JSON 직렬화 시 비밀번호가 응답에 포함되지 않도록 합니다. 
 *       비밀번호는 요청(deserialization)에서는 받을 수 있지만, 응답(serialization)에서는 제외됩니다.</li>
 * </ul>
 * 
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 요청 시 - 비밀번호 포함 가능
 * UserCreateRequest request = UserCreateRequest.builder()
 *     .name("홍길동")
 *     .email("hong@example.com")
 *     .password("securePassword123")
 *     .build();
 * 
 * // toString() 호출 시 - 비밀번호 제외됨
 * System.out.println(request); // UserCreateRequest(name=홍길동, email=hong@example.com)
 * 
 * // JSON 응답 시 - 비밀번호 제외됨
 * // {"name":"홍길동","email":"hong@example.com"}
 * }</pre>
 * 
 * <p><strong>주의사항</strong>: 이 DTO를 사용하는 모든 코드에서 비밀번호를 로그에 출력하거나 
 * 클라이언트에게 전송하지 않도록 주의해야 합니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
            message = "비밀번호는 영문과 숫자를 포함해야 합니다")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
