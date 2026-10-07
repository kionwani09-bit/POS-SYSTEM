package com.pos.service.impl;

import com.pos.db.SessionContext;
import com.pos.domain.Role;
import com.pos.domain.User;
import com.pos.dto.AuthResult;
import com.pos.exception.AccountLockedException;
import com.pos.exception.ValidationException;
import com.pos.repository.SessionRepository;
import com.pos.repository.UserRepository;
import com.pos.service.AuditService;
import com.pos.service.AuthService;
import com.pos.domain.AuditEventType;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class AuthServiceImpl implements AuthService {
    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class.getName());
    private static final int MAX_ATTEMPTS = 3;

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final AuditService auditService;

    // sessionId -> inactivity timer future
    private final Map<Long, ScheduledFuture<?>> inactivityTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pos-inactivity-timer");
            t.setDaemon(true);
            return t;
        });

    // Callback fired when a session times out (set by UI layer)
    private Runnable onSessionTimeout;

    public AuthServiceImpl(UserRepository userRepo, SessionRepository sessionRepo,
                           AuditService auditService) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.auditService = auditService;
    }

    public void setOnSessionTimeout(Runnable callback) {
        this.onSessionTimeout = callback;
    }

    @Override
    public AuthResult login(String username, String password) {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new ValidationException("Invalid username or password."));

        if (user.locked()) {
            auditService.log(AuditEventType.ACCOUNT_LOCKED, user.id(),
                "Login attempt on locked account: " + username, null, null);
            throw new AccountLockedException(username);
        }

        if (!BCrypt.checkpw(password, user.passwordHash())) {
            int attempts = user.failedAttempts() + 1;
            boolean nowLocked = attempts >= MAX_ATTEMPTS;
            userRepo.updateLoginState(user.id(), nowLocked, attempts, user.lastLogin());
            if (nowLocked) {
                auditService.log(AuditEventType.ACCOUNT_LOCKED, user.id(),
                    "Account locked after " + MAX_ATTEMPTS + " failed attempts", null, null);
                throw new AccountLockedException(username);
            }
            throw new ValidationException("Invalid username or password. Attempts: " + attempts);
        }

        // Successful login — reset attempts and expose the refreshed login timestamp.
        Instant loginTime = Instant.now();
        userRepo.updateLoginState(user.id(), false, 0, loginTime);
        user = new User(user.id(), user.username(), user.passwordHash(), user.role(),
            false, 0, loginTime, user.createdAt());

        long sessionId = sessionRepo.save(user.id());
        SessionContext.set(user, sessionId);

        auditService.log(AuditEventType.LOGIN, user.id(), "User logged in: " + username, null, null);

        scheduleInactivityTimer(sessionId, user.id(), username);

        return new AuthResult(user, sessionId);
    }

    @Override
    public void logout(long userId) {
        long sessionId = SessionContext.getCurrentSessionId();
        if (sessionId > 0) {
            cancelInactivityTimer(sessionId);
            sessionRepo.close(sessionId);
        }
        SessionContext.clear();
        auditService.log(AuditEventType.LOGOUT, userId, "User logged out", null, null);
    }

    @Override
    public void unlockAccount(long targetUserId, long adminId) {
        User admin = userRepo.findById(adminId)
            .orElseThrow(() -> new ValidationException("Admin user not found"));
        if (admin.role() != Role.ADMINISTRATOR) {
            throw new ValidationException("Only administrators can unlock accounts.");
        }
        User target = userRepo.findById(targetUserId)
            .orElseThrow(() -> new ValidationException("Target user not found"));
        userRepo.updateLoginState(targetUserId, false, 0, target.lastLogin());
        auditService.log(AuditEventType.ACCOUNT_UNLOCKED, adminId,
            "Account unlocked: userId=" + targetUserId, "locked", "unlocked");
    }

    /** Resets the inactivity timer — call this on every user action. */
    public void resetInactivityTimer(long sessionId, long userId, String username) {
        cancelInactivityTimer(sessionId);
        scheduleInactivityTimer(sessionId, userId, username);
    }

    private void scheduleInactivityTimer(long sessionId, long userId, String username) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            LOG.info("Session timeout for user: " + username);
            sessionRepo.close(sessionId);
            SessionContext.clear();
            if (onSessionTimeout != null) onSessionTimeout.run();
        }, 5, TimeUnit.MINUTES);
        inactivityTimers.put(sessionId, future);
    }

    private void cancelInactivityTimer(long sessionId) {
        ScheduledFuture<?> f = inactivityTimers.remove(sessionId);
        if (f != null) f.cancel(false);
    }

    /** Utility: hash a plaintext password with BCrypt. */
    public static String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(12));
    }
}
