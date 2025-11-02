package com.packed_go.consumption_service.dtos;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsumptionItemDTO {
    private Long consumptionId;
    private String consumptionName;
    private Integer quantity;
    private Double unitPrice;
}
