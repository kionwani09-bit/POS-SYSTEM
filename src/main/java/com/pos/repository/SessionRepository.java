package com.pos.repository;

import com.pos.db.DatabaseManager;

import java.sql.*;

public class SessionRepository {
    private final DatabaseManager db;

    public SessionRepository(DatabaseManager db) { this.db = db; }

    public long save(long userId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO sessions (user_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId); ps.executeUpdate(); c.commit();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return k.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Session insert failed");
    }

    public void updateShift(long sessionId, long shiftId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE sessions SET shift_id=? WHERE id=?")) {
            ps.setLong(1, shiftId); ps.setLong(2, sessionId); ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void close(long sessionId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE sessions SET logout_time=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setLong(1, sessionId); ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
