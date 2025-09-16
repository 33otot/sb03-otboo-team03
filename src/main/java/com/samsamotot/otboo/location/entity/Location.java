package com.samsamotot.otboo.location.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Check;
import jakarta.persistence.Convert;
import com.samsamotot.otboo.common.converter.StringListJsonbConverter;

import java.util.List;

@Builder
@Entity
@Table(name = "locations")
@Check(constraints = "latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "x", nullable = false)
    private int x;

    @Column(name = "y", nullable = false)
    private int y;

    @Convert(converter = StringListJsonbConverter.class)
    @Column(name = "location_names", nullable = false)
    private List<String> locationNames;
}
