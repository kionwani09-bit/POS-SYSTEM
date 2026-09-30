package com.pos;

import com.pos.db.DatabaseManager;
import com.pos.exception.ValidationException;
import com.pos.repository.*;
import com.pos.service.AuditService;
import com.pos.service.ConfigService;
import com.pos.service.impl.*;
import com.pos.ui.MainFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            db.initialize();

            SystemConfigRepository configRepo = new SystemConfigRepository(db);
            ConfigService configService = new ConfigServiceImpl(configRepo);
            configService.validateRequiredKeys();

            AuditLogRepository auditRepo = new AuditLogRepository(db);
            AuditService auditService = new AuditServiceImpl(auditRepo);

            // Minimal service wiring for startup UI bootstrap.
            UserRepository userRepo = new UserRepository(db);
            SessionRepository sessionRepo = new SessionRepository(db);
            AuthServiceImpl authService = new AuthServiceImpl(userRepo, sessionRepo, auditService);

            SwingUtilities.invokeLater(() -> {
                System.out.println("POS System starting...");
                new MainFrame(db, authService, configService).setVisible(true);
            });
        } catch (Exception e) {
            System.err.println("POS startup failed: " + e.getMessage());
            throw new RuntimeException("Critical startup configuration missing or invalid", e);
        }
    }
}
