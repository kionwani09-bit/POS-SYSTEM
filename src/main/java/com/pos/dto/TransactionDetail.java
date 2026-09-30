package com.pos.dto;
import com.pos.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record TransactionDetail(
    String transactionId,
    long cashierId,
    List<CartLineItem> lineItems,
    List<PaymentEntry> payments,
    BigDecimal grandTotal,
    Instant completedAt,
    TransactionStatus status
) {}
