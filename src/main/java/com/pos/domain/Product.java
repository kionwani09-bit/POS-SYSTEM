package com.pos.domain;
import java.math.BigDecimal;
import java.time.Instant;
public record Product(
    long id,
    String sku,
    String name,
    String description,
    BigDecimal unitPrice,
    long taxCategoryId,
    boolean taxExempt,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
