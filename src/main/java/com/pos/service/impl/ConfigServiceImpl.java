package com.pos.service.impl;

import com.pos.db.SessionContext;
import com.pos.dto.ConfigDto;
import com.pos.exception.ValidationException;
import com.pos.repository.SystemConfigRepository;
import com.pos.service.ConfigService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigServiceImpl implements ConfigService {

    private static final List<String> REQUIRED_KEYS = List.of(
        "store.name", "store.address", "session.timeout.minutes",
        "lowstock.threshold", "gateway.url", "printer.port"
    );

    private static final List<String> GATEWAY_KEYS = List.of("gateway.url", "gateway.key");

    private final SystemConfigRepository repo;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ConfigServiceImpl(SystemConfigRepository repo) {
        this.repo = repo;
    }

    /** Called at startup — loads all config into cache and validates required keys. */
    @Override
    public void validateRequiredKeys() {
        cache.putAll(repo.findAll());
        for (String key : REQUIRED_KEYS) {
            if (!cache.containsKey(key) || cache.get(key).isBlank()) {
                throw new ValidationException("Missing required configuration key: " + key);
            }
        }
    }

    @Override
    public String get(String key) {
        return cache.get(key);
    }

    @Override
    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    @Override
    public void update(ConfigDto dto, long adminUserId) {
        for (Map.Entry<String, String> entry : dto.settings().entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v == null || v.isBlank()) {
                throw new ValidationException("Config value for key '" + k + "' must not be blank.");
            }
            repo.save(k, v, adminUserId);
            // Gateway credential changes require restart — do not update cache
            if (!GATEWAY_KEYS.contains(k)) {
                cache.put(k, v);
            }
        }
    }
}
