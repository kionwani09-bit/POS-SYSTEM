package com.pos.service;
import com.pos.dto.report.*;
import java.nio.file.Path;
import java.time.LocalDate;
public interface ReportService {
    DailySalesReport getDailySalesReport(LocalDate date);
    ProductSalesReport getProductSalesReport(LocalDate from, LocalDate to);
    CashierPerformanceReport getCashierReport(long cashierId, LocalDate from, LocalDate to);
    ShiftSummaryReport getShiftSummaryReport(long shiftId);
    Path exportReportAsCsv(Object report);
}
