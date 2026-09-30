package com.pos.dto;
import java.util.List;
public record ImportResult(int successCount, int failureCount, List<String> errors) {}
