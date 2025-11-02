package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
