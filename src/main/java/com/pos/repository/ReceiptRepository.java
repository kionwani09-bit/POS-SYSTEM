package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.dto.Receipt;

import java.sql.*;
import java.util.Optional;

public class ReceiptRepository {
    private final DatabaseManager db;

    public ReceiptRepository(DatabaseManager db) { this.db = db; }

    public void saveInTx(Connection conn, long transactionDbId, String content) throws SQLException {
        String sql = "INSERT INTO receipts (transaction_id, content, printed) VALUES (?,?,FALSE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionDbId); ps.setString(2, content); ps.executeUpdate();
        }
    }

    public Optional<Receipt> findByTransactionId(String transactionId) {
        String sql = "SELECT r.*, t.transaction_id AS txn_str FROM receipts r "
            + "JOIN transactions t ON r.transaction_id=t.id WHERE t.transaction_id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new Receipt(
                    rs.getLong("id"), rs.getString("txn_str"),
                    rs.getString("content"), rs.getBoolean("printed"),
                    rs.getTimestamp("generated_at").toInstant()));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void markPrinted(long receiptId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE receipts SET printed=TRUE WHERE id=?")) {
            ps.setLong(1, receiptId); ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
