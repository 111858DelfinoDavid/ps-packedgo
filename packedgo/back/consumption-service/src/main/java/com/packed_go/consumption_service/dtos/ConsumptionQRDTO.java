package com.packed_go.consumption_service.dtos;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsumptionQRDTO {
    private Long detailId;
    private Long consumptionId;
    private String consumptionName;
    private String qrCode;
}
