package com.pos.service;
import com.pos.domain.AuditEntry;
import com.pos.domain.AuditEventType;
import java.time.Instant;
import java.util.List;
public interface AuditService {
    void log(AuditEventType eventType, long userId, String description, String previousValue, String newValue);
    List<AuditEntry> search(Long userId, AuditEventType eventType, Instant from, Instant to);
}
