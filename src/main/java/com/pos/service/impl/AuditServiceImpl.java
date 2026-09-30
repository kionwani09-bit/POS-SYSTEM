package com.pos.service.impl;

import com.pos.domain.AuditEntry;
import com.pos.domain.AuditEventType;
import com.pos.repository.AuditLogRepository;
import com.pos.service.AuditService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository repo;

    public AuditServiceImpl(AuditLogRepository repo) { this.repo = repo; }

    @Override
    public void log(AuditEventType eventType, long userId, String description,
                    String previousValue, String newValue) {
        Instant now = Instant.now();
        String checksum = sha256(eventType.name() + userId + now + description);
        AuditEntry entry = new AuditEntry(0, eventType, userId, now,
            description, previousValue, newValue, checksum);
        repo.save(entry);
    }

    @Override
    public List<AuditEntry> search(Long userId, AuditEventType eventType, Instant from, Instant to) {
        return repo.search(userId, eventType, from, to);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
