package com.pos.dto;
import java.util.Map;
public record RefundRequest(
    String originalTransactionId,
    Map<Long, Integer> lineItemQuantities,  // lineItemId -> qty to refund
    String paymentMethod
) {}
