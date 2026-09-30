package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.Shift;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class ShiftRepository {
    private final DatabaseManager db;

    public ShiftRepository(DatabaseManager db) { this.db = db; }

    public long save(Shift shift) {
        String sql = "INSERT INTO shifts (cashier_id,manager_id,start_time,opening_cash,status) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, shift.cashierId()); ps.setLong(2, shift.managerId());
            ps.setTimestamp(3, Timestamp.from(shift.startTime()));
            ps.setBigDecimal(4, shift.openingCash()); ps.setString(5, shift.status());
            ps.executeUpdate(); c.commit();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Shift insert failed");
    }

    public Optional<Shift> findById(long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM shifts WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public Optional<Shift> findActiveByUserId(long cashierId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM shifts WHERE cashier_id=? AND status='OPEN' LIMIT 1")) {
            ps.setLong(1, cashierId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void close(long shiftId, BigDecimal expectedClose, BigDecimal actualClose, BigDecimal variance) {
        String sql = "UPDATE shifts SET status='CLOSED',end_time=CURRENT_TIMESTAMP,"
            + "expected_closing_cash=?,actual_closing_cash=?,cash_variance=? WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, expectedClose); ps.setBigDecimal(2, actualClose);
            ps.setBigDecimal(3, variance); ps.setLong(4, shiftId);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Shift map(ResultSet rs) throws SQLException {
        Timestamp end = rs.getTimestamp("end_time");
        return new Shift(rs.getLong("id"), rs.getLong("cashier_id"), rs.getLong("manager_id"),
            rs.getTimestamp("start_time").toInstant(), end != null ? end.toInstant() : null,
            rs.getBigDecimal("opening_cash"), rs.getBigDecimal("expected_closing_cash"),
            rs.getBigDecimal("actual_closing_cash"), rs.getBigDecimal("cash_variance"),
            rs.getString("status"));
    }
}
