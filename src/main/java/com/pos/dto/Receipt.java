package com.pos.dto;
import java.time.Instant;
public record Receipt(
    long id,
    String transactionId,
    String content,
    boolean printed,
    Instant generatedAt
) {}
