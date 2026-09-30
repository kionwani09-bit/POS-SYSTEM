package com.pos.db;

import org.h2.jdbcx.JdbcDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;
    private JdbcDataSource dataSource;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void initialize() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(props.getProperty("db.url", "jdbc:h2:./pos-data;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"));
        ds.setUser(props.getProperty("db.username", "sa"));
        ds.setPassword(props.getProperty("db.password", ""));
        this.dataSource = ds;
        runSchema();
        LOG.info("Database initialized successfully.");
    }

    private void runSchema() {
        try (Connection conn = getRawConnection();
             InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) throw new RuntimeException("schema.sql not found on classpath");
            String sql = new String(is.readAllBytes());
            conn.setAutoCommit(false);
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement s = conn.createStatement()) {
                        s.execute(trimmed);
                    } catch (SQLException ex) {
                        LOG.fine("Schema stmt note: " + ex.getMessage());
                    }
                }
            }
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize schema: " + e.getMessage(), e);
        }
    }

    /** Returns a raw connection without setAutoCommit(false) — used only during schema init. */
    private Connection getRawConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource not set");
        return dataSource.getConnection();
    }

    /** Returns a connection with autoCommit=false for all application use. */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DatabaseManager not initialized. Call initialize() first.");
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }
}
