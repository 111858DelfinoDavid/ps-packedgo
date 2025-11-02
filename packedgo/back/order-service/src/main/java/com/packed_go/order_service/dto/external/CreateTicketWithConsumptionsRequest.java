package com.packed_go.order_service.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketWithConsumptionsRequest {
    private Long userId;
    private Long eventId;
    private List<TicketConsumptionDTO> consumptions;
}
