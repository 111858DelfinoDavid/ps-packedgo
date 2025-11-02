package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemDTO {
    private Long id;
    private Long eventId;
    private String eventName;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
    private List<OrderItemConsumptionDTO> consumptions;
}
