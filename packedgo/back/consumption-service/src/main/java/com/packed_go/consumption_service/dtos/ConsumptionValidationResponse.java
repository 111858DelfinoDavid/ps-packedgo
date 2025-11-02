package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsumptionValidationResponse {
    private boolean valid;
    private String message;
    private Long detailId;
    private Long ticketId;
    private Long consumptionId;
    private String consumptionName;
    private Integer quantityRedeemed;
    private Integer remainingQuantity;
    private LocalDateTime validatedAt;
}
