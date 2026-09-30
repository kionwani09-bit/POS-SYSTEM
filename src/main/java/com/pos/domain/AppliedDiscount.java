package com.pos.domain;
import java.math.BigDecimal;
public record AppliedDiscount(
    long discountId,
    String name,
    DiscountType type,
    DiscountScope scope,
    BigDecimal amountDeducted
) {}
