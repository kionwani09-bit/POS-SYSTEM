package com.pos.dto;
import com.pos.domain.User;
public record AuthResult(User user, long sessionId) {}
