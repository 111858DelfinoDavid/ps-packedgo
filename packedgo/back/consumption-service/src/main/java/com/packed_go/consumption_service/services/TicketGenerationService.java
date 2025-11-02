package com.packed_go.consumption_service.services;

import com.packed_go.consumption_service.clients.EventServiceClient;
import com.packed_go.consumption_service.clients.OrderServiceClient;
import com.packed_go.consumption_service.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketGenerationService {

    private final OrderServiceClient orderServiceClient;
    private final EventServiceClient eventServiceClient;
    private final QRCodeService qrCodeService;

    public GenerateTicketsResponse generateTicketsForOrder(Long orderId) {
        try {
            log.info("Starting ticket generation for order: {}", orderId);

            OrderDTO order = orderServiceClient.getOrderById(orderId);

            if (order == null) {
                return buildErrorResponse("Order not found");
            }

            if (!"PAID".equals(order.getStatus())) {
                return buildErrorResponse("Order must be in PAID status. Current: " + order.getStatus());
            }

            List<GeneratedTicketDTO> generatedTickets = new ArrayList<>();

            for (OrderItemDTO item : order.getItems()) {
                for (int i = 0; i < item.getQuantity(); i++) {
                    GeneratedTicketDTO ticket = generateSingleTicket(order, item);
                    generatedTickets.add(ticket);
                }
            }

            log.info("Successfully generated {} tickets for order {}", generatedTickets.size(), orderId);

            return GenerateTicketsResponse.builder()
                    .success(true)
                    .message("Tickets generated successfully")
                    .tickets(generatedTickets)
                    .build();

        } catch (Exception e) {
            log.error("Error generating tickets for order {}: {}", orderId, e.getMessage(), e);
            return buildErrorResponse("Failed to generate tickets: " + e.getMessage());
        }
    }

    private GeneratedTicketDTO generateSingleTicket(OrderDTO order, OrderItemDTO item) {
        List<ConsumptionItemDTO> consumptions = new ArrayList<>();
        if (item.getConsumptions() != null) {
            for (OrderItemConsumptionDTO consumption : item.getConsumptions()) {
                consumptions.add(ConsumptionItemDTO.builder()
                        .consumptionId(consumption.getConsumptionId())
                        .consumptionName(consumption.getConsumptionName())
                        .quantity(consumption.getQuantity())
                        .unitPrice(consumption.getUnitPrice())
                        .build());
            }
        }

        CreateTicketWithConsumptionsRequest createRequest = CreateTicketWithConsumptionsRequest.builder()
                .passId(null)
                .userId(order.getUserId())
                .consumptions(consumptions)
                .build();

        TicketWithConsumptionsResponse createdTicket = eventServiceClient.createTicketWithConsumptions(createRequest);

        String entryQR = qrCodeService.generateEntryQR(
                createdTicket.getTicketId(),
                order.getUserId(),
                item.getEventId()
        );

        List<ConsumptionQRDTO> consumptionQRs = new ArrayList<>();
        if (createdTicket.getConsumptions() != null) {
            for (TicketConsumptionDTO ticketConsumption : createdTicket.getConsumptions()) {
                if (ticketConsumption.getDetails() != null) {
                    for (TicketConsumptionDetailDTO detail : ticketConsumption.getDetails()) {
                        String consumptionQR = qrCodeService.generateConsumptionQR(
                                createdTicket.getTicketId(),
                                detail.getDetailId(),
                                order.getUserId(),
                                item.getEventId()
                        );

                        consumptionQRs.add(ConsumptionQRDTO.builder()
                                .detailId(detail.getDetailId())
                                .consumptionId(detail.getConsumptionId())
                                .consumptionName(detail.getConsumptionName())
                                .qrCode(consumptionQR)
                                .build());
                    }
                }
            }
        }

        return GeneratedTicketDTO.builder()
                .ticketId(createdTicket.getTicketId())
                .userId(order.getUserId())
                .eventId(item.getEventId())
                .eventName(item.getEventName())
                .entryQrCode(entryQR)
                .consumptionQRs(consumptionQRs)
                .build();
    }

    private GenerateTicketsResponse buildErrorResponse(String message) {
        return GenerateTicketsResponse.builder()
                .success(false)
                .message(message)
                .tickets(new ArrayList<>())
                .build();
    }
}