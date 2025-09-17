package com.samsamotot.otboo.feed.entity;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Weather;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

@Builder(toBuilder = true)
@Entity
@Table(name = "feeds")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weather_id")
    private Weather weather;

    @Column(name = "content", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private long likeCount = 0L;

    @Builder.Default
    @Column(name = "comment_count", nullable = false)
    private long commentCount = 0L;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedClothes> feedClothes = new ArrayList<>();

    public void addClothes(Clothes clothes) {
        if (clothes == null) return;
        if (this.feedClothes == null) this.feedClothes = new ArrayList<>();

        boolean exists = this.feedClothes.stream()
            .anyMatch(fc ->
                fc.getClothes().getId().equals(clothes.getId())
            );
        if (exists) return;

        FeedClothes fc = FeedClothes.builder()
            .feed(this)
            .clothes(clothes)
            .build();
        this.feedClothes.add(fc);
    }

    public void updateContent(String newContent) {
        if (newContent == null) return;
        if (!Objects.equals(this.content, newContent)) {
            this.content = newContent;
        }
    }

    public void delete() {
        this.isDeleted = true;
    }
}
