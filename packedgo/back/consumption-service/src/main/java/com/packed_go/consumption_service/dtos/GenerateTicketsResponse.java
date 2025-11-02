package com.packed_go.consumption_service.dtos;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerateTicketsResponse {
    private boolean success;
    private String message;
    private List<GeneratedTicketDTO> tickets;
}
