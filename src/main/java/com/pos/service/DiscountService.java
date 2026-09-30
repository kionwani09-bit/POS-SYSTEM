package com.pos.service;
import com.pos.domain.Cart;
import com.pos.domain.Discount;
import com.pos.dto.PromotionDto;
import java.util.List;
public interface DiscountService {
    void applyDiscountCode(Cart cart, String code);
    List<Discount> getActivePromotions();
    void createPromotion(PromotionDto dto);
}
