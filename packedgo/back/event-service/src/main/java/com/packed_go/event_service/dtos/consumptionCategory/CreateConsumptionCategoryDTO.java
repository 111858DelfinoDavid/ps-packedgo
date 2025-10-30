package com.packed_go.event_service.dtos.consumptionCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateConsumptionCategoryDTO {
    private String name;
    private Long createdBy;
}
