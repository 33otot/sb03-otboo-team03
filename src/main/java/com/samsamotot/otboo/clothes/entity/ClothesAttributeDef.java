package com.samsamotot.otboo.clothes.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * 의상 속성을 정의하는 엔티티 (계절, 재질, 색상 등...)
 */
@Builder
@Entity
@Table(name = "clothes_attribute_defs")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttributeDef extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClothesAttributeOption> options = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addOption(ClothesAttributeOption option) {
        options.add(option);
        option.updateDefinition(this);
    }

    public void removeOption(ClothesAttributeOption option) {
        options.remove(option);
        option.updateDefinition(null);
    }

    // 명시적으로 빌더 메서드 추가
    public static ClothesAttributeDef createClothesAttributeDef(String name) {
        return ClothesAttributeDef.builder()
            .name(name)
            .build();
    }

    // 정의 + 옵션 함께 생성
    public static ClothesAttributeDef createClothesAttributeDef(String name, List<String> optionValues) {
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .name(name)
            .build();

        optionValues.forEach(value -> {
            ClothesAttributeOption option = ClothesAttributeOption.builder()
                .value(value)
                .definition(def)
                .build();
            def.addOption(option);
        });

        return def;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateOptions(List<String> newValues) {
        this.options.clear();
        newValues.forEach(value -> {
            ClothesAttributeOption option = ClothesAttributeOption.createClothesAttributeOption(this, value);
            this.addOption(option);
        });
    }
}
