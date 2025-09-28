package com.samsamotot.otboo.weather.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "grids",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_grids_x_y", columnNames = {"x", "y"})
        })
public class Grid extends BaseEntity {

        @Column(name = "x", nullable = false)
        private int x;

        @Column(name = "y", nullable = false)
        private int y;

        @Builder
        public Grid(int x, int y) {
                this.x = x;
                this.y = y;
        }
}
