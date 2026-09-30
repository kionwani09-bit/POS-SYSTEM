package com.pos.repository;

import com.pos.db.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;

public class PriceHistoryRepository {
    private final DatabaseManager db;

    public PriceHistoryRepository(DatabaseManager db) { this.db = db; }

    public void save(long productId, BigDecimal previousPrice, BigDecimal newPrice, long userId) {
        String sql = "INSERT INTO price_history (product_id, previous_price, new_price, changed_by_user_id) VALUES (?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, productId); ps.setBigDecimal(2, previousPrice);
            ps.setBigDecimal(3, newPrice); ps.setLong(4, userId);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
