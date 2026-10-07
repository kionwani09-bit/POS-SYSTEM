package com.pos.service.impl;

import com.pos.db.DatabaseManager;
import com.pos.dto.report.*;
import com.pos.repository.TransactionRepository;
import com.pos.repository.UserRepository;
import com.pos.service.ReportService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

public class ReportServiceImpl implements ReportService {
    private static final Logger LOG = Logger.getLogger(ReportServiceImpl.class.getName());

    private final DatabaseManager db;
    private final UserRepository userRepo;

    public ReportServiceImpl(DatabaseManager db, UserRepository userRepo) {
        this.db = db;
        this.userRepo = userRepo;
    }

    @Override
    public DailySalesReport getDailySalesReport(LocalDate date) {
        String sql = "SELECT "
            + "COUNT(*) AS total_txn, "
            + "COALESCE(SUM(grand_total),0) AS revenue, "
            + "COALESCE(SUM(total_tax),0) AS tax, "
            + "COALESCE(SUM(total_discount),0) AS discount "
            + "FROM transactions "
            + "WHERE CAST(created_at AS DATE) = ? AND status = 'COMPLETED'";

        String refundSql = "SELECT COALESCE(SUM(refund_amount),0) AS refunds "
            + "FROM refunds WHERE CAST(processed_at AS DATE) = ?";

        try (Connection c = db.getConnection()) {
            int totalTxn = 0; BigDecimal revenue = BigDecimal.ZERO;
            BigDecimal tax = BigDecimal.ZERO; BigDecimal discount = BigDecimal.ZERO;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalTxn = rs.getInt("total_txn");
                        revenue  = rs.getBigDecimal("revenue");
                        tax      = rs.getBigDecimal("tax");
                        discount = rs.getBigDecimal("discount");
                    }
                }
            }

            BigDecimal refunds = BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement(refundSql)) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) refunds = rs.getBigDecimal("refunds");
                }
            }

            BigDecimal netSales = revenue.subtract(refunds);
            return new DailySalesReport(date, totalTxn, revenue, tax, discount, refunds, netSales);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public ProductSalesReport getProductSalesReport(LocalDate from, LocalDate to) {
        String sql = "SELECT p.sku, p.name, "
            + "SUM(li.quantity) AS units_sold, "
            + "SUM(li.line_total) AS revenue "
            + "FROM line_items li "
            + "JOIN products p ON li.product_id = p.id "
            + "JOIN transactions t ON li.transaction_id = t.id "
            + "WHERE CAST(t.created_at AS DATE) BETWEEN ? AND ? AND t.status='COMPLETED' "
            + "GROUP BY p.id, p.sku, p.name ORDER BY revenue DESC";

        List<ProductSalesReport.ProductSalesLine> lines = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new ProductSalesReport.ProductSalesLine(
                        rs.getString("sku"), rs.getString("name"),
                        rs.getInt("units_sold"), rs.getBigDecimal("revenue"), 0));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return new ProductSalesReport(from, to, lines);
    }

    @Override
    public CashierPerformanceReport getCashierReport(long cashierId, LocalDate from, LocalDate to) {
        String sql = "SELECT u.username, "
            + "COUNT(t.id) AS total_txn, "
            + "COALESCE(SUM(t.grand_total),0) AS revenue "
            + "FROM transactions t "
            + "JOIN users u ON t.cashier_id = u.id "
            + "WHERE t.cashier_id = ? "
            + "AND CAST(t.created_at AS DATE) BETWEEN ? AND ? "
            + "AND t.status='COMPLETED'";

        String refundSql = "SELECT COALESCE(SUM(r.refund_amount),0) AS refunds "
            + "FROM refunds r "
            + "JOIN transactions t ON r.original_transaction_id = t.id "
            + "WHERE t.cashier_id = ? AND CAST(r.processed_at AS DATE) BETWEEN ? AND ?";

        List<CashierPerformanceReport.CashierLine> lines = new ArrayList<>();
        try (Connection c = db.getConnection()) {
            String name = ""; int totalTxn = 0; BigDecimal revenue = BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, cashierId);
                ps.setDate(2, java.sql.Date.valueOf(from));
                ps.setDate(3, java.sql.Date.valueOf(to));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        name = rs.getString("username");
                        totalTxn = rs.getInt("total_txn");
                        revenue = rs.getBigDecimal("revenue");
                    }
                }
            }
            BigDecimal refunds = BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement(refundSql)) {
                ps.setLong(1, cashierId);
                ps.setDate(2, java.sql.Date.valueOf(from));
                ps.setDate(3, java.sql.Date.valueOf(to));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) refunds = rs.getBigDecimal("refunds");
                }
            }
            lines.add(new CashierPerformanceReport.CashierLine(name, totalTxn, revenue, refunds));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return new CashierPerformanceReport(from, to, lines);
    }

    @Override
    public ShiftSummaryReport getShiftSummaryReport(long shiftId) {
        String sql = "SELECT s.*, u.username FROM shifts s JOIN users u ON s.cashier_id=u.id WHERE s.id=?";

        String revSql = "SELECT "
            + "COALESCE(SUM(CASE WHEN p.payment_method='CASH' THEN p.amount ELSE 0 END),0) AS cash_rev, "
            + "COALESCE(SUM(CASE WHEN p.payment_method IN ('CREDIT_CARD','DEBIT_CARD') THEN p.amount ELSE 0 END),0) AS card_rev, "
            + "COALESCE(SUM(CASE WHEN p.payment_method='GIFT_CARD' THEN p.amount ELSE 0 END),0) AS gc_rev, "
            + "COUNT(DISTINCT t.id) AS total_txn "
            + "FROM payments p JOIN transactions t ON p.transaction_id=t.id "
            + "WHERE t.shift_id=? AND t.status='COMPLETED'";

        try (Connection c = db.getConnection()) {
            String cashierName = ""; java.time.Instant start = null, end = null;
            BigDecimal openCash = BigDecimal.ZERO, expClose = BigDecimal.ZERO,
                actClose = BigDecimal.ZERO, variance = BigDecimal.ZERO;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, shiftId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cashierName = rs.getString("username");
                        start = rs.getTimestamp("start_time").toInstant();
                        Timestamp endTs = rs.getTimestamp("end_time");
                        end = endTs != null ? endTs.toInstant() : null;
                        openCash = nvl(rs.getBigDecimal("opening_cash"));
                        expClose = nvl(rs.getBigDecimal("expected_closing_cash"));
                        actClose = nvl(rs.getBigDecimal("actual_closing_cash"));
                        variance = nvl(rs.getBigDecimal("cash_variance"));
                    }
                }
            }

            int totalTxn = 0; BigDecimal cashRev = BigDecimal.ZERO,
                cardRev = BigDecimal.ZERO, gcRev = BigDecimal.ZERO;
            BigDecimal discounts = BigDecimal.ZERO, refunds = BigDecimal.ZERO;

            try (PreparedStatement ps = c.prepareStatement(revSql)) {
                ps.setLong(1, shiftId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cashRev  = nvl(rs.getBigDecimal("cash_rev"));
                        cardRev  = nvl(rs.getBigDecimal("card_rev"));
                        gcRev    = nvl(rs.getBigDecimal("gc_rev"));
                        totalTxn = rs.getInt("total_txn");
                    }
                }
            }

            return new ShiftSummaryReport(shiftId, cashierName, start, end, totalTxn,
                cashRev, cardRev, gcRev, discounts, refunds,
                openCash, expClose, actClose, variance);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Path exportReportAsCsv(Object report) {
        try {
            Path tmp = Files.createTempFile("pos-report-", ".csv");
            try (BufferedWriter w = Files.newBufferedWriter(tmp)) {
                if (report instanceof DailySalesReport r) {
                    w.write("Date,Transactions,Revenue,Tax,Discounts,Refunds,NetSales\n");
                    w.write(r.date()+","+r.totalTransactions()+","+r.totalRevenue()+","
                        +r.totalTax()+","+r.totalDiscounts()+","+r.totalRefunds()+","+r.netSales()+"\n");
                } else if (report instanceof ProductSalesReport r) {
                    w.write("SKU,Name,UnitsSold,Revenue,RefundedUnits\n");
                    for (var l : r.lines())
                        w.write(l.sku()+","+l.name()+","+l.unitsSold()+","+l.revenue()+","+l.refundedUnits()+"\n");
                } else if (report instanceof CashierPerformanceReport r) {
                    w.write("Cashier,Transactions,Revenue,Refunds\n");
                    for (var l : r.lines())
                        w.write(l.cashierName()+","+l.totalTransactions()+","+l.totalRevenue()+","+l.totalRefunds()+"\n");
                } else if (report instanceof ShiftSummaryReport r) {
                    w.write("ShiftId,Cashier,Start,End,Transactions,CashRev,CardRev,GCRev,OpenCash,ExpClose,ActClose,Variance\n");
                    w.write(r.shiftId()+","+r.cashierName()+","+r.startTime()+","+r.endTime()+","
                        +r.totalTransactions()+","+r.cashRevenue()+","+r.cardRevenue()+","
                        +r.giftCardRevenue()+","+r.openingCash()+","+r.expectedClosingCash()+","
                        +r.actualClosingCash()+","+r.cashVariance()+"\n");
                } else {
                    w.write(report.toString());
                }
            }
            return tmp;
        } catch (IOException e) { throw new RuntimeException("CSV export failed", e); }
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
