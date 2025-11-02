package com.packed_go.order_service.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketConsumptionDTO {
    private Long ticketConsumptionId;
    private Integer quantity;
}
