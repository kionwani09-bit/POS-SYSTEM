package com.pos.domain;
import java.time.Instant;
public record Inventory(
    long id,
    long productId,
    int quantity,
    int lowStockThreshold,
    Instant updatedAt
) {}
