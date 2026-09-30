package com.pos.dto;
import com.pos.domain.DiscountType;
import com.pos.domain.DiscountScope;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PromotionDto(
    String code, String name, DiscountType type,
    BigDecimal value, DiscountScope scope,
    LocalDate startDate, LocalDate endDate
) {}
