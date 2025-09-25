package com.samsamotot.otboo.location.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import com.samsamotot.otboo.weather.entity.Grid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Builder
@Entity
@Table(name = "locations")
@Check(constraints = "latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", nullable = false)
    private Grid grid;

    @Column(name = "location_names", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> locationNames;
}
