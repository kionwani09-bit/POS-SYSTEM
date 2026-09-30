package com.pos.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompletedTransaction(
    String transactionId,
    long cashierId,
    long sessionId,
    long shiftId,
    List<CartLineItem> lineItems,
    List<PaymentEntry> payments,
    List<AppliedDiscount> appliedDiscounts,
    BigDecimal subtotal,
    BigDecimal totalTax,
    BigDecimal totalDiscount,
    BigDecimal grandTotal,
    Instant completedAt,
    TransactionStatus status
) {}
