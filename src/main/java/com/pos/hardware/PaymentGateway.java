package com.pos.hardware;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayResponse authorize(CardPaymentRequest request);

    record CardPaymentRequest(String cardReference, BigDecimal amount, String paymentMethod) {}

    record GatewayResponse(boolean approved, String authCode, String message) {}
}
