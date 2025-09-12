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

@Builder
@Entity
@Table(
    name = "clothes_attribute_options",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cao_definition_value", columnNames = {"definition_id", "value"}),
        @UniqueConstraint(name = "uq_cao_definition_id_id", columnNames = {"definition_id", "id"})
    }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttributeOption extends BaseEntity {

    @Column(name = "value", nullable = false)
    private String value;

    @ManyToOne
    @JoinColumn(name = "definition_id", nullable = false)
    private ClothesAttributeDef definition;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // 연관관계 편의 메서드
    public void updateDefinition(ClothesAttributeDef definition) {
        this.definition = definition;
    }

    // 명시적으로 빌더 메서드 추가
    public static ClothesAttributeOption createClothesAttributeOption(ClothesAttributeDef def, String value) {
        return ClothesAttributeOption.builder()
            .definition(def)
            .value(value)
            .build();
    }
}
