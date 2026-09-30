package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.AppliedDiscount;

import java.sql.*;
import java.util.List;

public class AppliedDiscountRepository {
    private final DatabaseManager db;

    public AppliedDiscountRepository(DatabaseManager db) { this.db = db; }

    public void saveAllInTx(Connection conn, long transactionDbId, List<AppliedDiscount> discounts) throws SQLException {
        String sql = "INSERT INTO applied_discounts (transaction_id, discount_id, amount_deducted, scope) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (AppliedDiscount d : discounts) {
                ps.setLong(1, transactionDbId); ps.setLong(2, d.discountId());
                ps.setBigDecimal(3, d.amountDeducted()); ps.setString(4, d.scope().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
