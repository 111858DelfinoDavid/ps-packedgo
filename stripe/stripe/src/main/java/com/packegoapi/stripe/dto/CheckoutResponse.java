package com.packegoapi.stripe.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutResponse {
    private String sessionId;
    private String url;
    private String externalReference;
}
