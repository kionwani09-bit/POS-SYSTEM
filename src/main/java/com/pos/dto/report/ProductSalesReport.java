package com.pos.dto.report;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record ProductSalesReport(LocalDate from, LocalDate to, List<ProductSalesLine> lines) {
    public record ProductSalesLine(String sku, String name, int unitsSold, BigDecimal revenue, int refundedUnits) {}
}
