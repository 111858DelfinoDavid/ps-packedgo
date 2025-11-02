package com.packed_go.consumption_service.dtos;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemConsumptionDTO {
    private Long id;
    private Long consumptionId;
    private String consumptionName;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
}
