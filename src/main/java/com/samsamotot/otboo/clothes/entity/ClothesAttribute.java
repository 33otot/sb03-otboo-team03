package com.samsamotot.otboo.clothes.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * 의상 속성 엔티티 (의상에 등록된 속성과 옵션을 관리, 계절: 봄, 재질: 면 등...)
 */
@Builder
@Entity
@Table(
    name = "clothes_attributes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_ca_clothes_definition", columnNames = {"clothes_id", "definition_id"})
    }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttribute extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @ManyToOne
    @JoinColumn(name = "definition_id", nullable = false)
    private ClothesAttributeDef definition;

    @Column(name = "value", nullable = false)
    private String value;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // 연관관계 편의 메서드
    public void updateClothes(Clothes clothes) {
        this.clothes = clothes;
    }

    public void updateValue(String value) {
        this.value = value;
    }

    // 명시적으로 빌더 메서드 추가
    public static ClothesAttribute createClothesAttribute(ClothesAttributeDef definition, String value) {
        return ClothesAttribute.builder()
            .definition(definition)
            .value(value)
            .build();
    }
}
