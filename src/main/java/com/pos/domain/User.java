package com.pos.domain;
import java.time.Instant;
public record User(
    long id,
    String username,
    String passwordHash,
    Role role,
    boolean locked,
    int failedAttempts,
    Instant lastLogin,
    Instant createdAt
) {}
