package com.packed_go.event_service.dtos.ticketConsumption;

import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TicketConsumptionDTO {
    List<TicketConsumptionDetailDTO> ticketDetails;

}
