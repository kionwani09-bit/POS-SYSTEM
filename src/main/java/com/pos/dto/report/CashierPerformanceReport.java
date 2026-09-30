package com.pos.dto.report;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record CashierPerformanceReport(LocalDate from, LocalDate to, List<CashierLine> lines) {
    public record CashierLine(String cashierName, int totalTransactions, BigDecimal totalRevenue, BigDecimal totalRefunds) {}
}
