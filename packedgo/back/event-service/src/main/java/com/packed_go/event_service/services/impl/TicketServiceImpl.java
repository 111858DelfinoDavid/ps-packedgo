package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.ticket.CreateTicketDTO;
import com.packed_go.event_service.dtos.ticket.TicketDTO;
import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.entities.Pass;
import com.packed_go.event_service.entities.Ticket;
import com.packed_go.event_service.entities.TicketConsumption;
import com.packed_go.event_service.repositories.PassRepository;
import com.packed_go.event_service.repositories.TicketConsumptionRepository;
import com.packed_go.event_service.repositories.TicketRepository;
import com.packed_go.event_service.services.TicketConsumptionService;
import com.packed_go.event_service.services.TicketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final PassRepository passRepository;
    private final TicketConsumptionRepository ticketConsumptionRepository;
    private final TicketConsumptionService ticketConsumptionService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketDTO createTicket(CreateTicketDTO createTicketDTO) {
        // Buscar el pass por código con bloqueo pesimista
        Pass pass = passRepository.findByCodeWithLock(createTicketDTO.getPassCode())
                .orElseThrow(() -> new RuntimeException("Pass with code " + createTicketDTO.getPassCode() + " not found"));

        // Verificar que el pass esté disponible
        if (!pass.isAvailable()) {
            throw new RuntimeException("Pass is not available for purchase");
        }

        // Crear el TicketConsumption
        TicketConsumptionDTO ticketConsumptionDTO = ticketConsumptionService.createWithDetails(createTicketDTO.getTicketConsumption());
        TicketConsumption ticketConsumption = modelMapper.map(ticketConsumptionDTO, TicketConsumption.class);

        // Crear el Ticket
        Ticket ticket = new Ticket(createTicketDTO.getUserId(), pass, ticketConsumption);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Marcar el pass como vendido
        pass.setSold(true);
        pass.setAvailable(false);
        pass.setSoldToUserId(createTicketDTO.getUserId());
        pass.setSoldAt(LocalDateTime.now());
        passRepository.save(pass);

        return modelMapper.map(savedTicket, TicketDTO.class);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketDTO purchaseTicket(Long userId, String passCode, Long ticketConsumptionId) {
        // Buscar el pass por código con bloqueo pesimista
        Pass pass = passRepository.findByCodeWithLock(passCode)
                .orElseThrow(() -> new RuntimeException("Pass with code " + passCode + " not found"));

        // Verificar que el pass esté disponible
        if (!pass.isAvailable()) {
            throw new RuntimeException("Pass is not available for purchase");
        }

        // Buscar el TicketConsumption
        TicketConsumption ticketConsumption = ticketConsumptionRepository.findById(ticketConsumptionId)
                .orElseThrow(() -> new RuntimeException("TicketConsumption with id " + ticketConsumptionId + " not found"));

        // Crear el Ticket
        Ticket ticket = new Ticket(userId, pass, ticketConsumption);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Marcar el pass como vendido
        pass.setSold(true);
        pass.setAvailable(false);
        pass.setSoldToUserId(userId);
        pass.setSoldAt(LocalDateTime.now());
        passRepository.save(pass);

        return modelMapper.map(savedTicket, TicketDTO.class);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketDTO redeemTicket(Long ticketId) {
        // Buscar el ticket con bloqueo pesimista
        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + ticketId + " not found"));

        // Verificar que el ticket esté activo
        if (!ticket.isActive()) {
            throw new RuntimeException("Ticket is not active");
        }

        // Verificar que no esté ya canjeado
        if (ticket.isRedeemed()) {
            throw new RuntimeException("Ticket is already redeemed");
        }

        // Marcar como canjeado
        ticket.setRedeemed(true);
        ticket.setRedeemedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);

        return modelMapper.map(savedTicket, TicketDTO.class);
    }

    @Override
    public TicketDTO findById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + id + " not found"));
        return modelMapper.map(ticket, TicketDTO.class);
    }

    @Override
    public TicketDTO findByPassCode(String passCode) {
        Ticket ticket = ticketRepository.findByPassCode(passCode)
                .orElseThrow(() -> new RuntimeException("Ticket with pass code " + passCode + " not found"));
        return modelMapper.map(ticket, TicketDTO.class);
    }

    @Override
    public List<TicketDTO> findByUserId(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, TicketDTO.class))
                .toList();
    }

    @Override
    public List<TicketDTO> findByUserIdAndActive(Long userId, boolean active) {
        List<Ticket> tickets = active ? 
                ticketRepository.findByUserIdAndActiveTrue(userId) : 
                ticketRepository.findByUserId(userId).stream()
                        .filter(ticket -> !ticket.isActive())
                        .toList();
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, TicketDTO.class))
                .toList();
    }

    @Override
    public List<TicketDTO> findByUserIdAndRedeemed(Long userId, boolean redeemed) {
        List<Ticket> tickets = redeemed ? 
                ticketRepository.findByUserIdAndRedeemedTrue(userId) : 
                ticketRepository.findByUserIdAndRedeemedFalse(userId);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, TicketDTO.class))
                .toList();
    }

    @Override
    public List<TicketDTO> findByEventId(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, TicketDTO.class))
                .toList();
    }

    @Override
    public Long countTicketsByEventId(Long eventId) {
        return ticketRepository.countTicketsByEventId(eventId);
    }

    @Override
    public Long countRedeemedTicketsByEventId(Long eventId) {
        return ticketRepository.countRedeemedTicketsByEventId(eventId);
    }

    @Override
    @Transactional
    public boolean isTicketRedeemed(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + ticketId + " not found"));
        return ticket.isRedeemed();
    }

    @Recover
    public TicketDTO recoverCreateTicket(Exception ex, CreateTicketDTO createTicketDTO) {
        throw new RuntimeException("No se pudo crear el ticket después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }

    @Recover
    public TicketDTO recoverPurchaseTicket(Exception ex, Long userId, String passCode, Long ticketConsumptionId) {
        throw new RuntimeException("No se pudo comprar el ticket después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }

    @Recover
    public TicketDTO recoverRedeemTicket(Exception ex, Long ticketId) {
        throw new RuntimeException("No se pudo canjear el ticket después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }
}
