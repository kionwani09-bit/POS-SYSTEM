package com.pos.hardware;

import java.math.BigDecimal;

public class MockPaymentGateway implements PaymentGateway {
    @Override
    public GatewayResponse authorize(CardPaymentRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return new GatewayResponse(false, "INVALID", "Amount must be positive");
        }

        return request.amount().compareTo(new BigDecimal("10000")) < 0
            ? new GatewayResponse(true, "AUTH-OK", "Approved")
            : new GatewayResponse(false, "DECLINED", "Card declined");
    }
}
