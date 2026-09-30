package com.pos.dto;
import java.math.BigDecimal;
public record ProductDto(
    String sku, String name, String description,
    BigDecimal unitPrice, long taxCategoryId, boolean taxExempt
) {}
