package com.packed_go.event_service.dtos.event;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventDTO {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private LocalDateTime eventDate;
    //    private Point location;
    private double lat;
    private double lng;
    private Integer maxCapacity;
    private Integer currentCapacity;
    private BigDecimal basePrice;
    private String imageUrl;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

    // Información de Pass
    private Integer totalPasses;
    private Integer availablePasses;
    private Integer soldPasses;
}
