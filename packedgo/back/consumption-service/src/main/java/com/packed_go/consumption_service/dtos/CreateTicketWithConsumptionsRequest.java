package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTicketWithConsumptionsRequest {
    private Long passId;
    private Long userId;
    private List<ConsumptionItemDTO> consumptions;
}
