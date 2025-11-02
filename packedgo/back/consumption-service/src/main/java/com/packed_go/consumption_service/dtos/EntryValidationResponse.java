package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EntryValidationResponse {
    private boolean valid;
    private String message;
    private Long ticketId;
    private Long userId;
    private Long eventId;
    private String eventName;
    private String userName;
    private LocalDateTime validatedAt;
}
