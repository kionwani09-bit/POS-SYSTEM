package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.TransactionStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class TransactionRepository {
    private final DatabaseManager db;

    public TransactionRepository(DatabaseManager db) { this.db = db; }

    /** Inserts a transaction header within an existing connection/transaction. Returns the generated PK. */
    public long saveInTx(Connection conn, String transactionId, long sessionId, long cashierId,
                         long shiftId, BigDecimal subtotal, BigDecimal totalTax,
                         BigDecimal totalDiscount, BigDecimal grandTotal,
                         TransactionStatus status) throws SQLException {
        String sql = "INSERT INTO transactions "
            + "(transaction_id,session_id,cashier_id,shift_id,subtotal,total_tax,total_discount,grand_total,status)"
            + " VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, transactionId); ps.setLong(2, sessionId); ps.setLong(3, cashierId);
            ps.setLong(4, shiftId); ps.setBigDecimal(5, subtotal); ps.setBigDecimal(6, totalTax);
            ps.setBigDecimal(7, totalDiscount); ps.setBigDecimal(8, grandTotal);
            ps.setString(9, status.name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        }
        throw new SQLException("Transaction insert returned no key");
    }

    public Optional<Map<String, Object>> findByTransactionId(String transactionId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM transactions WHERE transaction_id=?")) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rowToMap(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public List<Map<String, Object>> findByDateRange(LocalDate from, LocalDate to) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM transactions "
            + "WHERE CAST(created_at AS DATE) BETWEEN ? AND ? AND status='COMPLETED'";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rowToMap(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public List<Map<String, Object>> findByShiftId(long shiftId) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM transactions WHERE shift_id=? AND status='COMPLETED'")) {
            ps.setLong(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rowToMap(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("transactionId", rs.getString("transaction_id"));
        row.put("cashierId", rs.getLong("cashier_id"));
        row.put("sessionId", rs.getLong("session_id"));
        row.put("shiftId", rs.getLong("shift_id"));
        row.put("subtotal", rs.getBigDecimal("subtotal"));
        row.put("totalTax", rs.getBigDecimal("total_tax"));
        row.put("totalDiscount", rs.getBigDecimal("total_discount"));
        row.put("grandTotal", rs.getBigDecimal("grand_total"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", rs.getTimestamp("created_at").toInstant());
        return row;
    }
}
