package com.samsamotot.otboo.profile.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Builder
@Entity
@Table(name = "profiles")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Location location;

    @Column(name = "name", length = 32, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 32)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Builder.Default
    @Column(name = "temperature_sensitivity")
    private Double temperatureSensitivity = 3.0;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Setter
    @Builder.Default
    @Column(name = "weather_notification_enabled", nullable = false)
    private boolean weatherNotificationEnabled = true;

    public void update(ProfileUpdateRequest request, Location location, String newImageUrl) {
        this.name = request.name();
        if (this.user != null) {
            this.user.updateUserInfo(request.name());
        }

        if (request.gender() != null) {
            this.gender = request.gender();
        }
        if (request.birthDate() != null) {
            this.birthDate = request.birthDate();
        }
        if (request.location() != null) {
            this.location = location;
        }
        if (request.temperatureSensitivity() != null) {
            this.temperatureSensitivity = request.temperatureSensitivity();
        }

        this.profileImageUrl = newImageUrl;
    }
}
