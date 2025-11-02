package com.packed_go.consumption_service.dtos;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QRPayload {
    private String type;
    private Long ticketId;
    private Long detailId;
    private Long userId;
    private Long eventId;
    private Long expiresAt;
    private String hmac;
}
