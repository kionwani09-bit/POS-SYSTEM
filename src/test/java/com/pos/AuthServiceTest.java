package com.pos;

import com.pos.domain.Role;
import com.pos.domain.User;
import com.pos.exception.AccountLockedException;
import com.pos.exception.ValidationException;
import com.pos.repository.SessionRepository;
import com.pos.repository.UserRepository;
import com.pos.service.AuditService;
import com.pos.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void loginSuccessReturnsUserAndSession() {
        UserRepository userRepo = mock(UserRepository.class);
        SessionRepository sessionRepo = mock(SessionRepository.class);
        AuditService auditService = mock(AuditService.class);
        AuthServiceImpl authService = new AuthServiceImpl(userRepo, sessionRepo, auditService);

        User user = new User(
            7L, "alice", BCrypt.hashpw("secret", BCrypt.gensalt(12)),
            Role.CASHIER, false, 1, Instant.now(), Instant.now()
        );

        when(userRepo.findByUsername("alice")).thenReturn(java.util.Optional.of(user));
        when(sessionRepo.save(7L)).thenReturn(99L);

        var result = authService.login("alice", "secret");

        assertThat(result.user().username()).isEqualTo("alice");
        assertThat(result.sessionId()).isEqualTo(99L);
        verify(userRepo).updateLoginState(7L, false, 0, result.user().lastLogin());
        verify(auditService).log(any(), eq(7L), anyString(), isNull(), isNull());
    }

    @Test
    void loginFailureLocksAfterThirdAttempt() {
        UserRepository userRepo = mock(UserRepository.class);
        SessionRepository sessionRepo = mock(SessionRepository.class);
        AuditService auditService = mock(AuditService.class);
        AuthServiceImpl authService = new AuthServiceImpl(userRepo, sessionRepo, auditService);

        User user = new User(
            3L, "bob", BCrypt.hashpw("correct", BCrypt.gensalt(12)),
            Role.MANAGER, false, 2, Instant.now(), Instant.now()
        );

        when(userRepo.findByUsername("bob")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login("bob", "wrong-password"))
            .isInstanceOf(AccountLockedException.class);

        verify(userRepo).updateLoginState(3L, true, 3, user.lastLogin());
    }

    @Test
    void passwordHashIsBcryptAndNeverStoredInPlainText() {
        String hash = AuthServiceImpl.hashPassword("super-secret");

        assertThat(hash).isNotBlank();
        assertThat(hash).doesNotContain("super-secret");
        assertThat(hash).startsWith("$2a$");
        assertThat(BCrypt.checkpw("super-secret", hash)).isTrue();
    }
}
