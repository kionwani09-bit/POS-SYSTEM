package com.pos.dto;
import java.math.BigDecimal;
public record TransactionSummary(
    BigDecimal subtotal, BigDecimal totalTax,
    BigDecimal totalDiscount, BigDecimal grandTotal
) {}
