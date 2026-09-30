package com.pos.repository;

import com.pos.db.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;

public class RefundRepository {
    private final DatabaseManager db;

    public RefundRepository(DatabaseManager db) { this.db = db; }

    public long saveInTx(Connection conn, String refundId, long originalTxnDbId,
                          long managerId, BigDecimal amount, String paymentMethod) throws SQLException {
        String sql = "INSERT INTO refunds (refund_id,original_transaction_id,manager_id,refund_amount,payment_method)"
            + " VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, refundId); ps.setLong(2, originalTxnDbId); ps.setLong(3, managerId);
            ps.setBigDecimal(4, amount); ps.setString(5, paymentMethod);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        }
        throw new SQLException("Refund insert failed");
    }

    public void saveLineItemInTx(Connection conn, long refundDbId, long originalLineItemId,
                                  int qty, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO refund_line_items (refund_id,original_line_item_id,refunded_qty,refunded_amount)"
            + " VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, refundDbId); ps.setLong(2, originalLineItemId);
            ps.setInt(3, qty); ps.setBigDecimal(4, amount); ps.executeUpdate();
        }
    }
}
