package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.TicketConsumption;
import jakarta.transaction.Transactional;

import java.util.List;

public interface TicketConsumptionService {


    @Transactional
    TicketConsumptionDTO create(TicketConsumptionDTO ticketConsumptionDTO);

    @Transactional
    TicketConsumption update(Long id, TicketConsumptionDTO ticketConsumptionDTO);

    @Transactional
    TicketConsumption deleteLogical(Long id);

    boolean redeemTicketDetails();


    List<TicketConsumptionDetailDTO> getTicketConsumptionDetails(Long id);





}
