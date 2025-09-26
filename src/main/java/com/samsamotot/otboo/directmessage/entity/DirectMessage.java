package com.samsamotot.otboo.directmessage.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.entity
 * FileName     : DirectMessage
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Entity
@Table(name = "direct_messages")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class DirectMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id",nullable = false)
    User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id",nullable = false)
    User receiver;


    @Column(nullable = false, columnDefinition = "TEXT")
    String message;

    @Column(nullable = false, name = "is_read")
    @Builder.Default
    boolean isRead = false;

    public void markRead() { this.isRead = true; }
}
