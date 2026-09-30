package com.pos.repository;

import com.pos.db.DatabaseManager;

import java.sql.*;

public class InventoryAdjustmentRepository {
    private final DatabaseManager db;

    public InventoryAdjustmentRepository(DatabaseManager db) { this.db = db; }

    public void save(long productId, int previousQty, int newQty, String reason, long userId) {
        String sql = "INSERT INTO inventory_adjustments (product_id,previous_qty,new_qty,reason,user_id) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, productId); ps.setInt(2, previousQty); ps.setInt(3, newQty);
            ps.setString(4, reason); ps.setLong(5, userId);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
