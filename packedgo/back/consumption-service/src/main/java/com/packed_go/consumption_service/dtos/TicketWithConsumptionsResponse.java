package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketWithConsumptionsResponse {
    private Long ticketId;
    private Long passId;
    private Long userId;
    private Long eventId;
    private String eventName;
    private String status;
    private LocalDateTime purchaseDate;
    private LocalDateTime usedAt;
    private List<TicketConsumptionDTO> consumptions;
}
