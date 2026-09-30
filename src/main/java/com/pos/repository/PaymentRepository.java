package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.PaymentEntry;

import java.sql.*;
import java.util.*;

public class PaymentRepository {
    private final DatabaseManager db;

    public PaymentRepository(DatabaseManager db) { this.db = db; }

    public void saveAllInTx(Connection conn, long transactionDbId, List<PaymentEntry> payments) throws SQLException {
        String sql = "INSERT INTO payments (transaction_id, payment_method, amount, reference) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PaymentEntry p : payments) {
                ps.setLong(1, transactionDbId); ps.setString(2, p.method().name());
                ps.setBigDecimal(3, p.amount()); ps.setString(4, p.reference());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Map<String, Object>> findByTransactionDbId(long transactionDbId) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM payments WHERE transaction_id=?")) {
            ps.setLong(1, transactionDbId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("method", rs.getString("payment_method"));
                    row.put("amount", rs.getBigDecimal("amount"));
                    row.put("reference", rs.getString("reference"));
                    result.add(row);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }
}
