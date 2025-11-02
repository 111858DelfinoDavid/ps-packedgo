package com.packed_go.event_service.dtos.consumptionCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsumptionCategoryDTO {
    private Long id;
    private String name;
    private Long createdBy;

    public Long getCreatedBy() {
        return createdBy;
    }
}
