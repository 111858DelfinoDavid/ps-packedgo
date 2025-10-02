package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.ticket.CreateTicketDTO;
import com.packed_go.event_service.dtos.ticket.TicketDTO;
import com.packed_go.event_service.services.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-service/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(@RequestBody CreateTicketDTO createTicketDTO) {
        log.info("Creando ticket para usuario: {} con pass: {}", createTicketDTO.getUserId(), createTicketDTO.getPassCode());
        TicketDTO ticket = ticketService.createTicket(createTicketDTO);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/purchase")
    public ResponseEntity<TicketDTO> purchaseTicket(@RequestParam Long userId, @RequestParam String passCode, @RequestParam Long ticketConsumptionId) {
        log.info("Comprando ticket para usuario: {} con pass: {}", userId, passCode);
        TicketDTO ticket = ticketService.purchaseTicket(userId, passCode, ticketConsumptionId);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{ticketId}/redeem")
    public ResponseEntity<TicketDTO> redeemTicket(@PathVariable Long ticketId) {
        log.info("Canjeando ticket: {}", ticketId);
        TicketDTO ticket = ticketService.redeemTicket(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long ticketId) {
        log.info("Obteniendo ticket: {}", ticketId);
        TicketDTO ticket = ticketService.findById(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/pass-code/{passCode}")
    public ResponseEntity<TicketDTO> getTicketByPassCode(@PathVariable String passCode) {
        log.info("Obteniendo ticket con código de pass: {}", passCode);
        TicketDTO ticket = ticketService.findByPassCode(passCode);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketDTO>> getTicketsByUser(@PathVariable Long userId) {
        log.info("Obteniendo tickets para usuario: {}", userId);
        List<TicketDTO> tickets = ticketService.findByUserId(userId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<TicketDTO>> getActiveTicketsByUser(@PathVariable Long userId) {
        log.info("Obteniendo tickets activos para usuario: {}", userId);
        List<TicketDTO> tickets = ticketService.findByUserIdAndActive(userId, true);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/redeemed")
    public ResponseEntity<List<TicketDTO>> getRedeemedTicketsByUser(@PathVariable Long userId) {
        log.info("Obteniendo tickets canjeados para usuario: {}", userId);
        List<TicketDTO> tickets = ticketService.findByUserIdAndRedeemed(userId, true);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/not-redeemed")
    public ResponseEntity<List<TicketDTO>> getNotRedeemedTicketsByUser(@PathVariable Long userId) {
        log.info("Obteniendo tickets no canjeados para usuario: {}", userId);
        List<TicketDTO> tickets = ticketService.findByUserIdAndRedeemed(userId, false);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketDTO>> getTicketsByEvent(@PathVariable Long eventId) {
        log.info("Obteniendo tickets para evento: {}", eventId);
        List<TicketDTO> tickets = ticketService.findByEventId(eventId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<Long> getTicketsCountByEvent(@PathVariable Long eventId) {
        log.info("Contando tickets para evento: {}", eventId);
        Long count = ticketService.countTicketsByEventId(eventId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/event/{eventId}/redeemed/count")
    public ResponseEntity<Long> getRedeemedTicketsCountByEvent(@PathVariable Long eventId) {
        log.info("Contando tickets canjeados para evento: {}", eventId);
        Long count = ticketService.countRedeemedTicketsByEventId(eventId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{ticketId}/is-redeemed")
    public ResponseEntity<Boolean> isTicketRedeemed(@PathVariable Long ticketId) {
        log.info("Verificando si el ticket está canjeado: {}", ticketId);
        boolean isRedeemed = ticketService.isTicketRedeemed(ticketId);
        return ResponseEntity.ok(isRedeemed);
    }
}
