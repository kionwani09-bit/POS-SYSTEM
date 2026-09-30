package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.AuditEntry;
import com.pos.domain.AuditEventType;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {
    private final DatabaseManager db;

    public AuditLogRepository(DatabaseManager db) { this.db = db; }

    public void save(AuditEntry entry) {
        String sql = "INSERT INTO audit_log "
            + "(event_type,user_id,event_time,description,previous_value,new_value,checksum)"
            + " VALUES (?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entry.eventType().name());
            ps.setLong(2, entry.userId());
            ps.setTimestamp(3, Timestamp.from(entry.eventTime()));
            ps.setString(4, entry.description());
            ps.setString(5, entry.previousValue());
            ps.setString(6, entry.newValue());
            ps.setString(7, entry.checksum());
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<AuditEntry> search(Long userId, AuditEventType eventType, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
        if (userId != null)    { sql.append(" AND user_id=?");    params.add(userId); }
        if (eventType != null) { sql.append(" AND event_type=?"); params.add(eventType.name()); }
        if (from != null)      { sql.append(" AND event_time>=?"); params.add(Timestamp.from(from)); }
        if (to != null)        { sql.append(" AND event_time<=?"); params.add(Timestamp.from(to)); }
        sql.append(" ORDER BY event_time DESC");

        List<AuditEntry> result = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new AuditEntry(
                    rs.getLong("id"),
                    AuditEventType.valueOf(rs.getString("event_type")),
                    rs.getLong("user_id"),
                    rs.getTimestamp("event_time").toInstant(),
                    rs.getString("description"),
                    rs.getString("previous_value"),
                    rs.getString("new_value"),
                    rs.getString("checksum")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }
}
