package com.pos.service.impl;

import com.pos.db.DatabaseManager;
import com.pos.domain.AuditEventType;
import com.pos.domain.Role;
import com.pos.domain.Shift;
import com.pos.domain.User;
import com.pos.dto.report.ShiftSummaryReport;
import com.pos.exception.ValidationException;
import com.pos.repository.ShiftRepository;
import com.pos.repository.TransactionRepository;
import com.pos.repository.UserRepository;
import com.pos.service.AuditService;
import com.pos.service.ShiftService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ShiftServiceImpl implements ShiftService {
    private final DatabaseManager db;
    private final ShiftRepository shiftRepo;
    private final UserRepository userRepo;
    private final TransactionRepository transactionRepo;
    private final AuditService auditService;

    public ShiftServiceImpl(DatabaseManager db,
                           ShiftRepository shiftRepo,
                           UserRepository userRepo,
                           TransactionRepository transactionRepo,
                           AuditService auditService) {
        this.db = db;
        this.shiftRepo = shiftRepo;
        this.userRepo = userRepo;
        this.transactionRepo = transactionRepo;
        this.auditService = auditService;
    }

    @Override
    public Shift openShift(long cashierId, BigDecimal openingCash, long managerId) {
        if (openingCash == null || openingCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Opening cash must be zero or greater.");
        }

        User cashier = userRepo.findById(cashierId)
            .orElseThrow(() -> new ValidationException("Cashier not found: " + cashierId));
        User manager = userRepo.findById(managerId)
            .orElseThrow(() -> new ValidationException("Manager not found: " + managerId));

        if (cashier.role() != Role.CASHIER) {
            throw new ValidationException("User is not a cashier: " + cashierId);
        }
        if (manager.role() != Role.MANAGER && manager.role() != Role.ADMINISTRATOR) {
            throw new ValidationException("User is not authorized to open a shift: " + managerId);
        }

        shiftRepo.findActiveByUserId(cashierId)
            .ifPresent(existing -> {
                throw new ValidationException("Cashier already has an open shift: " + existing.id());
            });

        Shift shift = new Shift(
            0L,
            cashierId,
            managerId,
            Instant.now(),
            null,
            openingCash,
            null,
            null,
            null,
            "OPEN"
        );

        long shiftId = shiftRepo.save(shift);
        Shift saved = new Shift(shiftId, cashierId, managerId, shift.startTime(), null,
            openingCash, null, null, null, "OPEN");

        auditService.log(
            AuditEventType.SHIFT_OPENED,
            cashierId,
            "Shift opened for cashierId=" + cashierId + ", openingCash=" + openingCash,
            null,
            String.valueOf(shiftId)
        );

        return saved;
    }

    @Override
    public ShiftSummaryReport closeShift(long shiftId, BigDecimal actualCash, long managerId) {
        if (actualCash == null || actualCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Actual cash must be zero or greater.");
        }

        Shift shift = shiftRepo.findById(shiftId)
            .orElseThrow(() -> new ValidationException("Shift not found: " + shiftId));

        User manager = userRepo.findById(managerId).orElse(null);
        if (manager == null) {
            if (managerId != shift.managerId()) {
                throw new ValidationException("Manager not found: " + managerId);
            }
        } else if (manager.role() != Role.MANAGER && manager.role() != Role.ADMINISTRATOR) {
            throw new ValidationException("User is not authorized to close a shift: " + managerId);
        }

        List<Map<String, Object>> rows = transactionRepo.findByShiftId(shiftId);
        BigDecimal totalSales = rows.stream()
            .map(row -> row.get("grandTotal") instanceof BigDecimal value ? value : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedClosing = shift.openingCash().add(totalSales);
        BigDecimal variance = actualCash.subtract(expectedClosing);

        shiftRepo.close(shiftId, expectedClosing, actualCash, variance);

        String cashierName = userRepo.findById(shift.cashierId())
            .map(User::username)
            .orElse("unknown");

        auditService.log(
            AuditEventType.SHIFT_CLOSED,
            shift.cashierId(),
            "Shift closed: shiftId=" + shiftId + ", expectedClosing=" + expectedClosing + ", actualCash=" + actualCash + ", variance=" + variance,
            String.valueOf(shift.openingCash()),
            String.valueOf(actualCash)
        );

        return new ShiftSummaryReport(
            shiftId,
            cashierName,
            shift.startTime(),
            Instant.now(),
            rows.size(),
            totalSales,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            shift.openingCash(),
            expectedClosing,
            actualCash,
            variance
        );
    }
}
