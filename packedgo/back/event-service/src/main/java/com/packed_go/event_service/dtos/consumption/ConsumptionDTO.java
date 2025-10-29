package com.packed_go.event_service.dtos.consumption;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ConsumptionDTO {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Long createdBy;
    private boolean active;


}