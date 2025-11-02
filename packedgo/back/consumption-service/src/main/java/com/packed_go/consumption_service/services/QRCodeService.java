package com.packed_go.consumption_service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.packed_go.consumption_service.dtos.QRPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    @Value("${qr.secret:your-super-secret-qr-key-change-this-in-production}")
    private String qrSecret;

    @Value("${qr.expiration.hours:24}")
    private int qrExpirationHours;

    private final ObjectMapper objectMapper;

    public String generateEntryQR(Long ticketId, Long userId, Long eventId) {
        try {
            long expiresAt = Instant.now().plus(qrExpirationHours, ChronoUnit.HOURS).toEpochMilli();
            
            QRPayload payload = QRPayload.builder()
                    .type("ENTRY")
                    .ticketId(ticketId)
                    .userId(userId)
                    .eventId(eventId)
                    .expiresAt(expiresAt)
                    .build();

            String hmac = generateHMAC(payload);
            payload.setHmac(hmac);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return generateQRImage(jsonPayload);

        } catch (Exception e) {
            log.error("Error generating entry QR for ticket {}: {}", ticketId, e.getMessage());
            throw new RuntimeException("Failed to generate entry QR code", e);
        }
    }

    public String generateConsumptionQR(Long ticketId, Long detailId, Long userId, Long eventId) {
        try {
            long expiresAt = Instant.now().plus(qrExpirationHours, ChronoUnit.HOURS).toEpochMilli();
            
            QRPayload payload = QRPayload.builder()
                    .type("CONSUMPTION")
                    .ticketId(ticketId)
                    .detailId(detailId)
                    .userId(userId)
                    .eventId(eventId)
                    .expiresAt(expiresAt)
                    .build();

            String hmac = generateHMAC(payload);
            payload.setHmac(hmac);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            return generateQRImage(jsonPayload);

        } catch (Exception e) {
            log.error("Error generating consumption QR for detail {}: {}", detailId, e.getMessage());
            throw new RuntimeException("Failed to generate consumption QR code", e);
        }
    }

    public QRPayload validateAndDecodeQR(String qrBase64) {
        try {
            QRPayload payload = objectMapper.readValue(qrBase64, QRPayload.class);

            if (!validateHMAC(payload)) {
                throw new SecurityException("Invalid QR code signature");
            }

            if (Instant.now().toEpochMilli() > payload.getExpiresAt()) {
                throw new IllegalStateException("QR code has expired");
            }

            return payload;

        } catch (JsonProcessingException e) {
            log.error("Error decoding QR payload: {}", e.getMessage());
            throw new RuntimeException("Invalid QR code format", e);
        }
    }

    private String generateHMAC(QRPayload payload) {
        try {
            String data = String.format("%s:%d:%d:%d:%d",
                    payload.getType(),
                    payload.getTicketId(),
                    payload.getDetailId() != null ? payload.getDetailId() : 0,
                    payload.getUserId(),
                    payload.getExpiresAt()
            );

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(qrSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            
            byte[] hmacBytes = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hmacBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating HMAC: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR signature", e);
        }
    }

    private boolean validateHMAC(QRPayload payload) {
        String receivedHmac = payload.getHmac();
        payload.setHmac(null);
        String calculatedHmac = generateHMAC(payload);
        payload.setHmac(receivedHmac);
        return receivedHmac.equals(calculatedHmac);
    }

    private String generateQRImage(String data) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}