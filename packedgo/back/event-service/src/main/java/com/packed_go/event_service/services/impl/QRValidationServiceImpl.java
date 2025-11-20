package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.qr.*;
import com.packed_go.event_service.entities.*;
import com.packed_go.event_service.repositories.*;
import com.packed_go.event_service.services.QRValidationService;
import com.packed_go.event_service.services.TicketConsumptionDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRValidationServiceImpl implements QRValidationService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TicketConsumptionDetailRepository detailRepository;
    private final TicketConsumptionDetailService detailService;
    private final TicketConsumptionRepository ticketConsumptionRepository;

    // Formato QR único: PACKEDGO|T:ticketId|TC:ticketConsumptionId|E:eventId|U:userId|TS:timestamp
    // TC es opcional para entradas
    
    private Map<String, String> parseQR(String qrCode) {
        if (qrCode == null || !qrCode.startsWith("PACKEDGO|")) {
            return null;
        }
        Map<String, String> values = new HashMap<>();
        String[] parts = qrCode.split("\\|");
        for (String part : parts) {
            if (part.equals("PACKEDGO")) continue;
            int idx = part.indexOf(':');
            if (idx > 0) {
                String key = part.substring(0, idx);
                String value = part.substring(idx + 1);
                values.put(key, value);
            }
        }
        return values;
    }

    @Override
    @Transactional
    public ValidateEntryQRResponse validateEntryQR(ValidateEntryQRRequest request) {
        try {
            log.info("🎫 Validating entry QR: {}", request.getQrCode());

            // 1. Parsear QR code
            Map<String, String> qrData = parseQR(request.getQrCode());
            if (qrData == null || !qrData.containsKey("T") || !qrData.containsKey("E") || !qrData.containsKey("U")) {
                log.warn("❌ QR parsing failed or missing keys");
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Código QR inválido")
                        .build();
            }

            Long ticketId = Long.parseLong(qrData.get("T"));
            Long eventIdFromQR = Long.parseLong(qrData.get("E"));
            Long userId = Long.parseLong(qrData.get("U"));

            // 2. Validar que el evento del QR coincide con el evento solicitado
            if (!eventIdFromQR.equals(request.getEventId())) {
                log.warn("❌ Event mismatch: QR Event={} vs Request Event={}", eventIdFromQR, request.getEventId());
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Este ticket no corresponde a este evento")
                        .build();
            }

            // 3. Buscar el ticket
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (ticketOpt.isEmpty()) {
                log.warn("❌ Ticket not found: {}", ticketId);
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Ticket no encontrado")
                        .build();
            }

            Ticket ticket = ticketOpt.get();

            // 4. Verificar que el ticket esté activo
            if (!ticket.isActive()) {
                log.warn("❌ Ticket inactive: {}", ticketId);
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Ticket inactivo")
                        .build();
            }

            // 5. Verificar que el ticket pertenece al evento
            if (!ticket.getPass().getEvent().getId().equals(request.getEventId())) {
                log.warn("❌ Ticket event mismatch: Ticket Event={} vs Request Event={}", ticket.getPass().getEvent().getId(), request.getEventId());
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Ticket no válido para este evento")
                        .build();
            }

            Event event = ticket.getPass().getEvent();

            // 6. Verificar si ya fue usado (Single Entry)
            if (ticket.isRedeemed()) {
                log.warn("❌ Ticket already redeemed: {}", ticketId);
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Entrada ya utilizada el " + ticket.getRedeemedAt())
                        .ticketInfo(ValidateEntryQRResponse.TicketEntryInfo.builder()
                                .ticketId(ticket.getId())
                                .userId(ticket.getUserId())
                                .customerName("Usuario " + userId)
                                .eventName(event.getName())
                                .passType(ticket.getPass().getCode())
                                .alreadyUsed(true)
                                .build())
                        .build();
            }

            // 7. Marcar como usado
            ticket.setRedeemed(true);
            ticket.setRedeemedAt(java.time.LocalDateTime.now());
            ticketRepository.save(ticket);

            // 8. Construir respuesta exitosa
            log.info("✅ Ticket valid and redeemed: {}", ticketId);

            return ValidateEntryQRResponse.builder()
                    .valid(true)
                    .message("✅ Entrada autorizada")
                    .ticketInfo(ValidateEntryQRResponse.TicketEntryInfo.builder()
                            .ticketId(ticket.getId())
                            .userId(ticket.getUserId())
                            .customerName("Usuario " + userId) // TODO: Integrar con users-service
                            .eventName(event.getName())
                            .passType(ticket.getPass().getCode()) // Pass code instead of name
                            .alreadyUsed(false)
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error validating entry QR", e);
            return ValidateEntryQRResponse.builder()
                    .valid(false)
                    .message("❌ Error al validar el código QR: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public ValidateConsumptionQRResponse validateConsumptionQR(ValidateConsumptionQRRequest request) {
        try {
            log.info("🍺 Validating consumption QR: {}", request.getQrCode());

            // 1. Parsear QR code para obtener ticketConsumptionId
            Map<String, String> qrData = parseQR(request.getQrCode());
            if (qrData == null || !qrData.containsKey("T") || !qrData.containsKey("E")) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Código QR inválido")
                        .build();
            }

            Long ticketId = Long.parseLong(qrData.get("T"));
            Long eventIdFromQR = Long.parseLong(qrData.get("E"));

            // 2. Validar que el evento del QR coincide
            if (!eventIdFromQR.equals(request.getEventId())) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Este ticket no corresponde a este evento")
                        .build();
            }

            // 3. NOTA: Este endpoint ahora requiere que el frontend envíe también el detailId
            // en el request después de que el empleado seleccione qué consumición canjear
            // El QR solo sirve para obtener el ticketConsumptionId
            if (request.getDetailId() == null) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Debe especificar qué consumición desea canjear")
                        .build();
            }

            Long detailId = request.getDetailId();

            // 3. Buscar el detalle de consumición
            Optional<TicketConsumptionDetail> detailOpt = detailRepository.findById(detailId);
            if (detailOpt.isEmpty()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Consumición no encontrada")
                        .build();
            }

            TicketConsumptionDetail detail = detailOpt.get();

            // 4. Verificar que esté activo
            if (!detail.isActive()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Consumición inactiva")
                        .build();
            }

            // 5. Verificar que no esté completamente canjeado
            if (detail.isRedeem()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Esta consumición ya fue canjeada completamente")
                        .build();
            }

            // 6. Determinar cantidad a canjear
            Integer quantityToRedeem = request.getQuantity() != null ? request.getQuantity() : 1;

            if (quantityToRedeem > detail.getQuantity()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Cantidad solicitada (" + quantityToRedeem + ") excede la disponible (" + detail.getQuantity() + ")")
                        .build();
            }

            // 7. Canjear la consumición (parcial o total)
            if (quantityToRedeem.equals(detail.getQuantity())) {
                // Canje total
                detailService.redeemDetail(detailId);

                return ValidateConsumptionQRResponse.builder()
                        .success(true)
                        .message("✅ Consumición canjeada exitosamente")
                        .consumptionInfo(ValidateConsumptionQRResponse.ConsumptionRedeemInfo.builder()
                                .detailId(detail.getId())
                                .consumptionId(detail.getConsumption().getId())
                                .consumptionName(detail.getConsumption().getName())
                                .consumptionType(detail.getConsumption().getCategory() != null ?
                                    detail.getConsumption().getCategory().getName() : "Sin categoría")
                                .quantityRedeemed(quantityToRedeem)
                                .remainingQuantity(0)
                                .fullyRedeemed(true)
                                .eventName(getEventNameFromDetail(detail))
                                .build())
                        .build();
            } else {
                // Canje parcial
                var updatedDetail = detailService.redeemDetailPartial(detailId, quantityToRedeem);

                Integer originalQuantity = detail.getQuantity() + quantityToRedeem;
                return ValidateConsumptionQRResponse.builder()
                        .success(true)
                        .message("✅ Consumición parcial canjeada (" + quantityToRedeem + " de " + originalQuantity + ")")
                        .consumptionInfo(ValidateConsumptionQRResponse.ConsumptionRedeemInfo.builder()
                                .detailId(detail.getId())
                                .consumptionId(detail.getConsumption().getId())
                                .consumptionName(detail.getConsumption().getName())
                                .consumptionType(detail.getConsumption().getCategory() != null ?
                                    detail.getConsumption().getCategory().getName() : "Sin categoría")
                                .quantityRedeemed(quantityToRedeem)
                                .remainingQuantity(updatedDetail.getQuantity())
                                .fullyRedeemed(updatedDetail.isRedeem())
                                .eventName(getEventNameFromDetail(detail))
                                .build())
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Error validating consumption QR", e);
            return ValidateConsumptionQRResponse.builder()
                    .success(false)
                    .message("❌ Error al validar el código QR: " + e.getMessage())
                    .build();
        }
    }

    private String getEventNameFromDetail(TicketConsumptionDetail detail) {
        try {
            // Obtener el ticket padre para llegar al evento
            TicketConsumption ticketConsumption = detail.getTicketConsumption();
            Optional<Ticket> ticketOpt = ticketRepository.findByTicketConsumption(ticketConsumption);

            if (ticketOpt.isPresent()) {
                return ticketOpt.get().getPass().getEvent().getName();
            }
            return "Evento desconocido";
        } catch (Exception e) {
            log.warn("Could not retrieve event name for detail {}", detail.getId());
            return "Evento desconocido";
        }
    }
}
