package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.Role;
import com.pos.domain.User;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class UserRepository {
    private final DatabaseManager db;

    public UserRepository(DatabaseManager db) { this.db = db; }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public long save(User user) {
        String sql = "INSERT INTO users (username, password_hash, role, locked, failed_attempts) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.username());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.role().name());
            ps.setBoolean(4, user.locked());
            ps.setInt(5, user.failedAttempts());
            ps.executeUpdate();
            c.commit();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) return k.getLong(1);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Failed to insert user");
    }

    public void updateLoginState(long userId, boolean locked, int failedAttempts, Instant lastLogin) {
        String sql = "UPDATE users SET locked=?, failed_attempts=?, last_login=? WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, locked);
            ps.setInt(2, failedAttempts);
            ps.setTimestamp(3, lastLogin != null ? Timestamp.from(lastLogin) : null);
            ps.setLong(4, userId);
            ps.executeUpdate();
            c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private User map(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getBoolean("locked"),
            rs.getInt("failed_attempts"),
            lastLogin != null ? lastLogin.toInstant() : null,
            createdAt != null ? createdAt.toInstant() : Instant.now()
        );
    }
}
