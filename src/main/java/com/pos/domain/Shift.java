package com.pos.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Shift(
    long id,
    long cashierId,
    long managerId,
    Instant startTime,
    Instant endTime,
    BigDecimal openingCash,
    BigDecimal expectedClosingCash,
    BigDecimal actualClosingCash,
    BigDecimal cashVariance,
    String status
) {}
