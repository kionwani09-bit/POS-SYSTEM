package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository {
    private final DatabaseManager db;

    public ProductRepository(DatabaseManager db) { this.db = db; }

    public Optional<Product> findBySku(String sku) {
        return queryOne("SELECT * FROM products WHERE sku = ? AND active = TRUE",
            ps -> ps.setString(1, sku));
    }

    public Optional<Product> findById(long id) {
        return queryOne("SELECT * FROM products WHERE id = ?",
            ps -> ps.setLong(1, id));
    }

    public List<Product> search(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return queryList(
            "SELECT * FROM products WHERE active = TRUE AND (LOWER(sku) LIKE ? OR LOWER(name) LIKE ?)",
            ps -> { ps.setString(1, pattern); ps.setString(2, pattern); });
    }

    public List<Product> findAll() {
        return queryList("SELECT * FROM products WHERE active = TRUE", ps -> {});
    }

    public long save(Product p) {
        String sql = "INSERT INTO products (sku,name,description,unit_price,tax_category_id,tax_exempt,active) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.sku()); ps.setString(2, p.name()); ps.setString(3, p.description());
            ps.setBigDecimal(4, p.unitPrice()); ps.setLong(5, p.taxCategoryId());
            ps.setBoolean(6, p.taxExempt()); ps.setBoolean(7, p.active());
            ps.executeUpdate(); c.commit();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Product insert failed");
    }

    public void update(Product p) {
        String sql = "UPDATE products SET name=?,description=?,unit_price=?,tax_category_id=?,tax_exempt=?,updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.name()); ps.setString(2, p.description());
            ps.setBigDecimal(3, p.unitPrice()); ps.setLong(4, p.taxCategoryId());
            ps.setBoolean(5, p.taxExempt()); ps.setLong(6, p.id());
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void softDelete(long productId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE products SET active=FALSE, updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setLong(1, productId); ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<Product> queryOne(String sql, SqlSetter setter) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    private List<Product> queryList(String sql, SqlSetter setter) {
        List<Product> result = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    private Product map(ResultSet rs) throws SQLException {
        return new Product(
            rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
            rs.getString("description"), rs.getBigDecimal("unit_price"),
            rs.getLong("tax_category_id"), rs.getBoolean("tax_exempt"), rs.getBoolean("active"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    @FunctionalInterface interface SqlSetter { void set(PreparedStatement ps) throws SQLException; }
}
