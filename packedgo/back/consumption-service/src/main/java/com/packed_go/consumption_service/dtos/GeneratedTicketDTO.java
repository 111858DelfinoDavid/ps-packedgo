package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneratedTicketDTO {
    private Long ticketId;
    private Long userId;
    private Long eventId;
    private String eventName;
    private String entryQrCode;
    private List<ConsumptionQRDTO> consumptionQRs;
}
