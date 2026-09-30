package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.Inventory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryRepository {
    private final DatabaseManager db;

    public InventoryRepository(DatabaseManager db) { this.db = db; }

    public Optional<Inventory> findByProductId(long productId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM inventory WHERE product_id = ?")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void updateQuantity(long productId, int newQty) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE inventory SET quantity=?, updated_at=CURRENT_TIMESTAMP WHERE product_id=?")) {
            ps.setInt(1, newQty); ps.setLong(2, productId);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Used inside an existing transaction — does NOT commit. */
    public void updateQuantityInTx(Connection conn, long productId, int newQty) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                 "UPDATE inventory SET quantity=?, updated_at=CURRENT_TIMESTAMP WHERE product_id=?")) {
            ps.setInt(1, newQty); ps.setLong(2, productId); ps.executeUpdate();
        }
    }

    public List<Inventory> findLowStock() {
        List<Inventory> result = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM inventory WHERE quantity <= low_stock_threshold");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public List<Inventory> findAll() {
        List<Inventory> result = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM inventory");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public void save(long productId, int quantity, int lowStockThreshold) {
        String sql = "MERGE INTO inventory (product_id, quantity, low_stock_threshold) KEY(product_id) VALUES (?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, productId); ps.setInt(2, quantity); ps.setInt(3, lowStockThreshold);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Inventory map(ResultSet rs) throws SQLException {
        return new Inventory(rs.getLong("id"), rs.getLong("product_id"),
            rs.getInt("quantity"), rs.getInt("low_stock_threshold"),
            rs.getTimestamp("updated_at").toInstant());
    }
}
