package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private LocalDateTime eventDate;
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;
    private Integer maxCapacity;
    private Integer currentCapacity;
    private BigDecimal basePrice;
    private String imageUrl;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

}
