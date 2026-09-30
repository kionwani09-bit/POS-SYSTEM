package com.pos.exception;
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String username) {
        super("Account is locked: " + username);
    }
}
