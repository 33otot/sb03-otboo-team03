package com.samsamotot.otboo.clothes.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Builder
@Entity
@Table(name = "clothes")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothes extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url", columnDefinition = "TEXT", nullable = true)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ClothesType type;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClothesAttribute> attributes = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addAttribute(ClothesAttribute attribute) {
        attributes.add(attribute);
        attribute.updateClothes(this);
    }

    public void removeAttribute(ClothesAttribute attribute) {
        attributes.remove(attribute);
        attribute.updateClothes(null);
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateType(ClothesType type) {
        this.type = type;
    }

    // 명시적으로 빌더 메서드 추가
    public static Clothes createClothes(String name, ClothesType type, User owner) {
        return Clothes.builder()
            .name(name)
            .type(type)
            .owner(owner)
            .build();
    }

}
