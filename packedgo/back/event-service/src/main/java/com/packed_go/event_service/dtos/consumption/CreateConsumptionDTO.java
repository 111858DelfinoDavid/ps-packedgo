package com.packed_go.event_service.dtos.consumption;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateConsumptionDTO {


    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    
    // Este campo NO se envía desde frontend, se inyecta desde JWT en el controller
    private Long createdBy;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private boolean active=true;


}
