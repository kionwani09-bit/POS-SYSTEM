package com.pos.dto;
import java.math.BigDecimal;
public record RefundResult(String refundId, BigDecimal refundAmount, String paymentMethod) {}
