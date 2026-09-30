package com.pos.domain;

import java.time.Instant;

public record AuditEntry(
    long id,
    AuditEventType eventType,
    long userId,
    Instant eventTime,
    String description,
    String previousValue,
    String newValue,
    String checksum
) {}
