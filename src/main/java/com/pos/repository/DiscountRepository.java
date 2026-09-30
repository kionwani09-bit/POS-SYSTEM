package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscountRepository {
    private final DatabaseManager db;

    public DiscountRepository(DatabaseManager db) { this.db = db; }

    public Optional<Discount> findByCode(String code) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM discounts WHERE code=?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public List<Discount> findActive() {
        List<Discount> result = new ArrayList<>();
        String sql = "SELECT * FROM discounts WHERE active=TRUE AND start_date<=CURRENT_DATE AND end_date>=CURRENT_DATE";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public long save(Discount d) {
        String sql = "INSERT INTO discounts (code,name,discount_type,value,scope,start_date,end_date,active) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.code()); ps.setString(2, d.name());
            ps.setString(3, d.type().name()); ps.setBigDecimal(4, d.value());
            ps.setString(5, d.scope().name()); ps.setDate(6, Date.valueOf(d.startDate()));
            ps.setDate(7, Date.valueOf(d.endDate())); ps.setBoolean(8, d.active());
            ps.executeUpdate(); c.commit();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Discount insert failed");
    }

    private Discount map(ResultSet rs) throws SQLException {
        return new Discount(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
            DiscountType.valueOf(rs.getString("discount_type")), rs.getBigDecimal("value"),
            DiscountScope.valueOf(rs.getString("scope")),
            rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(),
            rs.getBoolean("active"));
    }
}
