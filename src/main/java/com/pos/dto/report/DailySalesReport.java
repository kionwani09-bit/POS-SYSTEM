package com.pos.dto.report;
import java.math.BigDecimal;
import java.time.LocalDate;
public record DailySalesReport(
    LocalDate date,
    int totalTransactions,
    BigDecimal totalRevenue,
    BigDecimal totalTax,
    BigDecimal totalDiscounts,
    BigDecimal totalRefunds,
    BigDecimal netSales
) {}
