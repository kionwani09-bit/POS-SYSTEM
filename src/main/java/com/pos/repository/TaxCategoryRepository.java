package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.TaxCategory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaxCategoryRepository {
    private final DatabaseManager db;

    public TaxCategoryRepository(DatabaseManager db) { this.db = db; }

    public List<TaxCategory> findAll() {
        List<TaxCategory> result = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM tax_categories WHERE active=TRUE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public Optional<TaxCategory> findById(long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM tax_categories WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public long save(TaxCategory tc) {
        String sql = "INSERT INTO tax_categories (name, rate, active) VALUES (?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tc.name()); ps.setBigDecimal(2, tc.rate()); ps.setBoolean(3, tc.active());
            ps.executeUpdate(); c.commit();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("TaxCategory insert failed");
    }

    public void update(TaxCategory tc) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE tax_categories SET name=?, rate=?, active=? WHERE id=?")) {
            ps.setString(1, tc.name()); ps.setBigDecimal(2, tc.rate());
            ps.setBoolean(3, tc.active()); ps.setLong(4, tc.id());
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private TaxCategory map(ResultSet rs) throws SQLException {
        return new TaxCategory(rs.getLong("id"), rs.getString("name"),
            rs.getBigDecimal("rate"), rs.getBoolean("active"));
    }
}
