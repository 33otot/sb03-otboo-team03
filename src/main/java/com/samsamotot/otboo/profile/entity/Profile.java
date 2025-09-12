package com.samsamotot.otboo.profile.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.location.entity.Location;
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

    @Column(name = "temperature_sensitivity")
    private Double temperatureSensitivity;
}
