package com.pos.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class TransactionManager {
    private static final Logger LOG = Logger.getLogger(TransactionManager.class.getName());
    private final DatabaseManager db;

    public TransactionManager(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Executes the given work inside a single JDBC transaction.
     * Commits on success, rolls back on any exception and rethrows.
     */
    public <T> T executeInTransaction(TransactionalWork<T> work) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    LOG.severe("Rollback failed: " + re.getMessage());
                }
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not obtain database connection", e);
        }
    }

    @FunctionalInterface
    public interface TransactionalWork<T> {
        T execute(Connection conn) throws Exception;
    }
}
