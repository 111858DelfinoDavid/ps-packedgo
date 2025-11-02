package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketConsumptionDetailDTO {
    private Long detailId;
    private Long ticketConsumptionId;
    private Long consumptionId;
    private String consumptionName;
    private String qrCode;
    private String status;
    private LocalDateTime redeemedAt;
    private Long redeemedBy;
}
