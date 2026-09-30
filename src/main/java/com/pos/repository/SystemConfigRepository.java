package com.pos.repository;

import com.pos.db.DatabaseManager;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SystemConfigRepository {
    private final DatabaseManager db;

    public SystemConfigRepository(DatabaseManager db) { this.db = db; }

    public Optional<String> findByKey(String key) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT config_value FROM system_config WHERE config_key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public Map<String, String> findAll() {
        Map<String, String> result = new LinkedHashMap<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT config_key, config_value FROM system_config");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString("config_key"), rs.getString("config_value"));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public void save(String key, String value, long userId) {
        String sql = "MERGE INTO system_config (config_key, config_value, updated_by_user_id) KEY(config_key) VALUES (?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key); ps.setString(2, value); ps.setLong(3, userId);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
