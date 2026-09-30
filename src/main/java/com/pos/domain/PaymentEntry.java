package com.pos.domain;
import java.math.BigDecimal;
public record PaymentEntry(
    PaymentMethod method,
    BigDecimal amount,
    String reference
) {}
