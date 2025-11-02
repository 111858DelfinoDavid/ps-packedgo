package com.packed_go.consumption_service.clients;

import com.packed_go.consumption_service.dtos.CreateTicketWithConsumptionsRequest;
import com.packed_go.consumption_service.dtos.TicketConsumptionDetailDTO;
import com.packed_go.consumption_service.dtos.TicketWithConsumptionsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "event-service", url = "${event.service.url:http://localhost:8086}")
public interface EventServiceClient {

    /**
     * Crea un ticket con sus consumiciones asociadas en EVENT-SERVICE
     */
    @PostMapping("/api/event-service/tickets/create-with-consumptions")
    TicketWithConsumptionsResponse createTicketWithConsumptions(
            @RequestBody CreateTicketWithConsumptionsRequest request);

    /**
     * Obtiene un ticket completo con todas sus consumiciones
     */
    @GetMapping("/api/event-service/tickets/{ticketId}/full")
    TicketWithConsumptionsResponse getTicketFull(@PathVariable("ticketId") Long ticketId);

    /**
     * Obtiene el detalle de una consumición específica de un ticket
     */
    @GetMapping("/api/event-service/tickets/consumptions/{detailId}")
    TicketConsumptionDetailDTO getConsumptionDetail(@PathVariable("detailId") Long detailId);

    /**
     * Redime parcialmente una consumición (usado cuando quantity > 1)
     */
    @PutMapping("/api/event-service/tickets/consumptions/{detailId}/redeem-partial")
    TicketConsumptionDetailDTO redeemConsumptionPartial(
            @PathVariable("detailId") Long detailId,
            @RequestParam("quantity") Integer quantityToRedeem);

    /**
     * Redime completamente un ticket de entrada (marca como USED)
     */
    @PutMapping("/api/event-service/tickets/{ticketId}/redeem")
    TicketWithConsumptionsResponse redeemTicket(@PathVariable("ticketId") Long ticketId);
}