package com.samsamotot.otboo.follow.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PackageName  : com.samsamotot.otboo.follow.entity
 * FileName     : Follow
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"follower_id", "followee_id"})
    }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Follow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User followerId; // 사용자를 팔로우

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followeeId; // 사용자가 팔로우
}
