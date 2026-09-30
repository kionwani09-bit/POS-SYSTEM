package com.pos.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record GiftCard(
    long id,
    String cardNumber,
    BigDecimal balance,
    Instant issuedAt,
    Instant expiresAt
) {}
