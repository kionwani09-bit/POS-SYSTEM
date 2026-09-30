package com.pos.domain;
import java.math.BigDecimal;
import java.time.LocalDate;
public record Discount(
    long id,
    String code,
    String name,
    DiscountType type,
    BigDecimal value,
    DiscountScope scope,
    LocalDate startDate,
    LocalDate endDate,
    boolean active
) {}
