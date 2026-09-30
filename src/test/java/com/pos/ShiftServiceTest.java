package com.pos;

import com.pos.db.DatabaseManager;
import com.pos.domain.*;
import com.pos.dto.report.ShiftSummaryReport;
import com.pos.repository.*;
import com.pos.service.AuditService;
import com.pos.service.impl.ShiftServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ShiftServiceTest {

    @Test
    void openShiftCreatesOpenShiftAndAuditLog() {
        ShiftRepository shiftRepo = mock(ShiftRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        TransactionRepository transactionRepo = mock(TransactionRepository.class);
        AuditService auditService = mock(AuditService.class);

        when(userRepo.findById(10L)).thenReturn(Optional.of(new User(10L, "cashier", "hash", Role.CASHIER, false, 0, Instant.now(), Instant.now())));
        when(userRepo.findById(20L)).thenReturn(Optional.of(new User(20L, "manager", "hash", Role.MANAGER, false, 0, Instant.now(), Instant.now())));
        when(shiftRepo.save(any(Shift.class))).thenReturn(99L);

        ShiftServiceImpl service = new ShiftServiceImpl(DatabaseManager.getInstance(), shiftRepo, userRepo, transactionRepo, auditService);

        Shift shift = service.openShift(10L, new BigDecimal("150.00"), 20L);

        assertThat(shift.status()).isEqualTo("OPEN");
        assertThat(shift.openingCash()).isEqualByComparingTo("150.00");
        verify(shiftRepo).save(any(Shift.class));
        verify(auditService).log(eq(AuditEventType.SHIFT_OPENED), eq(10L), contains("Shift opened"), isNull(), contains("99"));
    }

    @Test
    void closeShiftCalculatesVarianceAndSummary() {
        ShiftRepository shiftRepo = mock(ShiftRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        TransactionRepository transactionRepo = mock(TransactionRepository.class);
        AuditService auditService = mock(AuditService.class);

        Shift existing = new Shift(7L, 10L, 20L, Instant.parse("2026-09-28T09:00:00Z"), null,
            new BigDecimal("100.00"), null, null, null, "OPEN");
        when(shiftRepo.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepo.findById(10L)).thenReturn(Optional.of(new User(10L, "cashier", "hash", Role.CASHIER, false, 0, Instant.now(), Instant.now())));
        when(transactionRepo.findByShiftId(7L)).thenReturn(List.of(
            Map.of("grandTotal", new BigDecimal("60.00"), "totalDiscount", new BigDecimal("5.00"), "totalTax", new BigDecimal("4.00"), "cashierId", 10L),
            Map.of("grandTotal", new BigDecimal("40.00"), "totalDiscount", new BigDecimal("0.00"), "totalTax", new BigDecimal("3.00"), "cashierId", 10L)
        ));

        ShiftServiceImpl service = new ShiftServiceImpl(DatabaseManager.getInstance(), shiftRepo, userRepo, transactionRepo, auditService);

        ShiftSummaryReport report = service.closeShift(7L, new BigDecimal("200.00"), 20L);

        assertThat(report.expectedClosingCash()).isEqualByComparingTo("180.00");
        assertThat(report.cashVariance()).isEqualByComparingTo("20.00");
        verify(shiftRepo).close(eq(7L), eq(new BigDecimal("180.00")), eq(new BigDecimal("200.00")), eq(new BigDecimal("20.00")));
    }
}
