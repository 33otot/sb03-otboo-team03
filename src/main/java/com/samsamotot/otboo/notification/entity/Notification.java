package com.samsamotot.otboo.notification.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PackageName  : com.samsamotot.otboo.notification.entity
 * FileName     : Notification
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Entity
@Table(name = "notifications")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false,length = 255)
    private String title;

    @Column(nullable = false,length = 255)
    private String content;

    @Column(nullable = false,length = 10)
    private NotificationLevel level;
}


