package com.pos.domain;
import java.math.BigDecimal;
public record TaxCategory(
    long id,
    String name,
    BigDecimal rate,
    boolean active
) {}
