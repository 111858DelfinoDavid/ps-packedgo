package com.packed_go.consumption_service.services;

import com.packed_go.consumption_service.clients.EventServiceClient;
import com.packed_go.consumption_service.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketValidationService {

    private final QRCodeService qrCodeService;
    private final EventServiceClient eventServiceClient;

    public EntryValidationResponse validateEntry(ValidateEntryRequest request) {
        try {
            QRPayload payload = qrCodeService.validateAndDecodeQR(request.getQrCode());

            if (!"ENTRY".equals(payload.getType())) {
                return buildErrorEntryResponse("Invalid QR type: expected ENTRY");
            }

            TicketWithConsumptionsResponse ticket = eventServiceClient.getTicketFull(payload.getTicketId());

            if (ticket == null) {
                return buildErrorEntryResponse("Ticket not found");
            }

            if (!ticket.getUserId().equals(payload.getUserId())) {
                return buildErrorEntryResponse("Ticket ownership mismatch");
            }

            if (!ticket.getEventId().equals(payload.getEventId())) {
                return buildErrorEntryResponse("Event mismatch");
            }

            if ("USED".equals(ticket.getStatus())) {
                return buildErrorEntryResponse("Ticket already used at: " + ticket.getUsedAt());
            }

            if ("CANCELLED".equals(ticket.getStatus())) {
                return buildErrorEntryResponse("Ticket has been cancelled");
            }

            if ("EXPIRED".equals(ticket.getStatus())) {
                return buildErrorEntryResponse("Ticket has expired");
            }

            TicketWithConsumptionsResponse updatedTicket = eventServiceClient.redeemTicket(payload.getTicketId());

            return EntryValidationResponse.builder()
                    .valid(true)
                    .message("Entry validated successfully")
                    .ticketId(ticket.getTicketId())
                    .userId(ticket.getUserId())
                    .eventId(ticket.getEventId())
                    .eventName(ticket.getEventName())
                    .userName("User #" + ticket.getUserId())
                    .validatedAt(LocalDateTime.now())
                    .build();

        } catch (SecurityException e) {
            log.error("Security validation failed: {}", e.getMessage());
            return buildErrorEntryResponse("Invalid or tampered QR code");
        } catch (IllegalStateException e) {
            log.error("State validation failed: {}", e.getMessage());
            return buildErrorEntryResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Error validating entry: {}", e.getMessage(), e);
            return buildErrorEntryResponse("Validation error: " + e.getMessage());
        }
    }

    public ConsumptionValidationResponse validateConsumption(ValidateConsumptionRequest request) {
        try {
            QRPayload payload = qrCodeService.validateAndDecodeQR(request.getQrCode());

            if (!"CONSUMPTION".equals(payload.getType())) {
                return buildErrorConsumptionResponse("Invalid QR type: expected CONSUMPTION");
            }

            if (payload.getDetailId() == null) {
                return buildErrorConsumptionResponse("Missing consumption detail ID");
            }

            TicketConsumptionDetailDTO detail = eventServiceClient.getConsumptionDetail(payload.getDetailId());

            if (detail == null) {
                return buildErrorConsumptionResponse("Consumption detail not found");
            }

            if ("REDEEMED".equals(detail.getStatus())) {
                return buildErrorConsumptionResponse("Consumption already redeemed at: " + detail.getRedeemedAt());
            }

            Integer quantityToRedeem = request.getQuantity() != null ? request.getQuantity() : 1;

            TicketConsumptionDetailDTO updatedDetail = eventServiceClient.redeemConsumptionPartial(
                    payload.getDetailId(),
                    quantityToRedeem
            );

            return ConsumptionValidationResponse.builder()
                    .valid(true)
                    .message("Consumption redeemed successfully")
                    .detailId(detail.getDetailId())
                    .ticketId(payload.getTicketId())
                    .consumptionId(detail.getConsumptionId())
                    .consumptionName(detail.getConsumptionName())
                    .quantityRedeemed(quantityToRedeem)
                    .remainingQuantity(0)
                    .validatedAt(LocalDateTime.now())
                    .build();

        } catch (SecurityException e) {
            log.error("Security validation failed: {}", e.getMessage());
            return buildErrorConsumptionResponse("Invalid or tampered QR code");
        } catch (IllegalStateException e) {
            log.error("State validation failed: {}", e.getMessage());
            return buildErrorConsumptionResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Error validating consumption: {}", e.getMessage(), e);
            return buildErrorConsumptionResponse("Validation error: " + e.getMessage());
        }
    }

    private EntryValidationResponse buildErrorEntryResponse(String message) {
        return EntryValidationResponse.builder()
                .valid(false)
                .message(message)
                .validatedAt(LocalDateTime.now())
                .build();
    }

    private ConsumptionValidationResponse buildErrorConsumptionResponse(String message) {
        return ConsumptionValidationResponse.builder()
                .valid(false)
                .message(message)
                .validatedAt(LocalDateTime.now())
                .build();
    }
}