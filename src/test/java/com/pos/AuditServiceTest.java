package com.pos;

import com.pos.domain.AuditEntry;
import com.pos.domain.AuditEventType;
import com.pos.repository.AuditLogRepository;
import com.pos.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    @Test
    void logCreatesChecksumAndPersistsEntry() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditServiceImpl service = new AuditServiceImpl(repo);

        service.log(AuditEventType.LOGIN, 5L, "User logged in", "old", "new");

        verify(repo).save(argThat(entry ->
            entry.eventType() == AuditEventType.LOGIN
                && entry.userId() == 5L
                && entry.description().equals("User logged in")
                && entry.previousValue().equals("old")
                && entry.newValue().equals("new")
                && entry.checksum() != null
                && entry.checksum().length() == 64
        ));
    }

    @Test
    void searchFiltersByCriteria() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditServiceImpl service = new AuditServiceImpl(repo);
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-30T00:00:00Z");

        when(repo.search(7L, AuditEventType.TRANSACTION_COMPLETED, from, to))
            .thenReturn(List.of(new AuditEntry(1L, AuditEventType.TRANSACTION_COMPLETED, 7L,
                Instant.now(), "Completed", null, null, "abc")));

        List<AuditEntry> result = service.search(7L, AuditEventType.TRANSACTION_COMPLETED, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventType()).isEqualTo(AuditEventType.TRANSACTION_COMPLETED);
        verify(repo).search(7L, AuditEventType.TRANSACTION_COMPLETED, from, to);
    }
}
