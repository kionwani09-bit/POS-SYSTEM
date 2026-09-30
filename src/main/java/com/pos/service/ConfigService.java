package com.pos.service;
import com.pos.dto.ConfigDto;
public interface ConfigService {
    String get(String key);
    String get(String key, String defaultValue);
    void update(ConfigDto dto, long adminUserId);
    void validateRequiredKeys();
}
