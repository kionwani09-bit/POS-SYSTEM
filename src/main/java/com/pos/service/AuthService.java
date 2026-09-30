package com.pos.service;
import com.pos.dto.AuthResult;
public interface AuthService {
    AuthResult login(String username, String password);
    void logout(long userId);
    void unlockAccount(long userId, long adminId);
}
