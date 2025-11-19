package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.qr.ValidateConsumptionQRRequest;
import com.packed_go.event_service.dtos.qr.ValidateConsumptionQRResponse;
import com.packed_go.event_service.dtos.qr.ValidateEntryQRRequest;
import com.packed_go.event_service.dtos.qr.ValidateEntryQRResponse;

public interface QRValidationService {

    /**
     * Valida un QR code de entrada al evento (ticket/pass)
     */
    ValidateEntryQRResponse validateEntryQR(ValidateEntryQRRequest request);

    /**
     * Valida y canjea un QR code de consumición
     */
    ValidateConsumptionQRResponse validateConsumptionQR(ValidateConsumptionQRRequest request);
}
