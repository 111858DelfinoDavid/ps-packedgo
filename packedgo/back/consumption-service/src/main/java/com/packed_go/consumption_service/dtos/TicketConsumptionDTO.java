package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketConsumptionDTO {
    private Long ticketConsumptionId;
    private Long consumptionId;
    private String consumptionName;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private String status;
    private List<TicketConsumptionDetailDTO> details;
}
