package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.ticket.CreateTicketDTO;
import com.packed_go.event_service.dtos.ticket.TicketDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface TicketService {

    @Transactional
    TicketDTO createTicket(CreateTicketDTO createTicketDTO);

    @Transactional
    TicketDTO purchaseTicket(Long userId, String passCode, Long ticketConsumptionId);

    @Transactional
    TicketDTO redeemTicket(Long ticketId);

    TicketDTO findById(Long id);

    TicketDTO findByPassCode(String passCode);

    List<TicketDTO> findByUserId(Long userId);

    List<TicketDTO> findByUserIdAndActive(Long userId, boolean active);

    List<TicketDTO> findByUserIdAndRedeemed(Long userId, boolean redeemed);

    List<TicketDTO> findByEventId(Long eventId);

    Long countTicketsByEventId(Long eventId);

    Long countRedeemedTicketsByEventId(Long eventId);

    @Transactional
    boolean isTicketRedeemed(Long ticketId);
}
