package com.samsamotot.otboo.weather.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Builder
@Entity
@Table(name = "weathers",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_weathers_grid_forecast",
        columnNames = {"grid_id", "forecast_at", "forecasted_at"}
    )
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weather extends BaseEntity {

    @Column(name = "forecast_at", nullable = false)
    private Instant forecastAt;

    @Column(name = "forecasted_at", nullable = false)
    private Instant forecastedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Grid grid;

    @Enumerated(EnumType.STRING)
    @Column(name = "sky_status", length = 32)
    private SkyStatus skyStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "precipitation_type", length = 32) // PTY
    private Precipitation precipitationType;
    
    @Column(name = "precipitation_amount") // PCP
    private Double precipitationAmount;
    
    @Column(name = "precipitation_prob") // POP
    private Double precipitationProbability;
    
    @Column(name = "humidity_current") // REH
    private Double humidityCurrent;

    @Column(name = "humidity_compared")
    private Double humidityComparedToDayBefore;

    @Column(name = "temperature_current") // TMP
    private Double temperatureCurrent;

    @Column(name = "temperature_compared")
    private Double temperatureComparedToDayBefore;

    @Column(name = "temperature_min") // TMN
    private Double temperatureMin;

    @Column(name = "temperature_max") // TMX
    private Double temperatureMax;

    @Column(name = "wind_speed") // WSD
    private Double windSpeed;

    @Enumerated(EnumType.STRING)
    @Column(name = "wind_as_word", length = 32)
    private WindAsWord windAsWord;

}
