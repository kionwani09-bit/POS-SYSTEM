package com.pos.service.impl;

import com.pos.domain.*;
import com.pos.dto.PromotionDto;
import com.pos.exception.InvalidDiscountException;
import com.pos.repository.DiscountRepository;
import com.pos.service.AuditService;
import com.pos.service.DiscountService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepo;
    private final AuditService auditService;
    private final long currentUserId;

    public DiscountServiceImpl(DiscountRepository discountRepo,
                                AuditService auditService,
                                long currentUserId) {
        this.discountRepo = discountRepo;
        this.auditService = auditService;
        this.currentUserId = currentUserId;
    }

    @Override
    public void applyDiscountCode(Cart cart, String code) {
        Discount discount = discountRepo.findByCode(code)
            .orElseThrow(() -> new InvalidDiscountException(code));

        LocalDate today = LocalDate.now();
        if (!discount.active()
                || today.isBefore(discount.startDate())
                || today.isAfter(discount.endDate())) {
            throw new InvalidDiscountException(code);
        }

        BigDecimal amount = computeDiscountAmount(discount, cart);

        // Clamp: total discounts must not push grand total below zero
        BigDecimal currentGrand = cart.getGrandTotal();
        if (amount.compareTo(currentGrand) > 0) {
            amount = currentGrand;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        AppliedDiscount applied = new AppliedDiscount(
            discount.id(), discount.name(), discount.type(), discount.scope(), amount);

        if (discount.scope() == DiscountScope.TRANSACTION) {
            cart.addTransactionDiscount(applied);
        } else {
            // LINE_ITEM scope: apply proportionally to all line items
            applyToLineItems(cart, discount, amount);
            return; // already added to line items
        }

        auditService.log(AuditEventType.DISCOUNT_APPLIED, currentUserId,
            "Discount applied: " + code + " amount=" + amount, null, code);
    }

    @Override
    public List<Discount> getActivePromotions() {
        return discountRepo.findActive();
    }

    @Override
    public void createPromotion(PromotionDto dto) {
        Discount d = new Discount(0, dto.code(), dto.name(), dto.type(), dto.value(),
            dto.scope(), dto.startDate(), dto.endDate(), true);
        discountRepo.save(d);
    }

    /** Auto-apply all active promotions to a newly added line item. */
    public void autoApplyPromotions(Cart cart, CartLineItem newItem) {
        List<Discount> active = discountRepo.findActive();
        for (Discount d : active) {
            if (d.scope() == DiscountScope.LINE_ITEM) {
                BigDecimal lineAmount = computeLineItemDiscount(d, newItem);
                if (lineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Clamp to line total
                    lineAmount = lineAmount.min(newItem.getLineTotal());
                    newItem.addDiscount(new AppliedDiscount(
                        d.id(), d.name(), d.type(), d.scope(), lineAmount));
                }
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BigDecimal computeDiscountAmount(Discount discount, Cart cart) {
        if (discount.type() == DiscountType.FIXED_AMOUNT) {
            return discount.value().setScale(2, RoundingMode.HALF_UP);
        }
        // PERCENTAGE — apply to subtotal
        return cart.getSubtotal()
            .multiply(discount.value().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeLineItemDiscount(Discount discount, CartLineItem item) {
        if (discount.type() == DiscountType.FIXED_AMOUNT) {
            return discount.value().setScale(2, RoundingMode.HALF_UP);
        }
        return item.getPreTaxTotal()
            .multiply(discount.value().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private void applyToLineItems(Cart cart, Discount discount, BigDecimal totalAmount) {
        List<CartLineItem> items = cart.getLineItems();
        if (items.isEmpty()) return;

        // Spread evenly; remainder goes to last item
        BigDecimal perItem = totalAmount.divide(
            BigDecimal.valueOf(items.size()), 2, RoundingMode.FLOOR);
        BigDecimal remaining = totalAmount;

        for (int i = 0; i < items.size(); i++) {
            CartLineItem item = items.get(i);
            BigDecimal portion = (i == items.size() - 1) ? remaining : perItem;
            portion = portion.min(item.getLineTotal());
            if (portion.compareTo(BigDecimal.ZERO) > 0) {
                item.addDiscount(new AppliedDiscount(
                    discount.id(), discount.name(), discount.type(), discount.scope(), portion));
                remaining = remaining.subtract(portion);
            }
        }
        auditService.log(AuditEventType.DISCOUNT_APPLIED, currentUserId,
            "Line discount applied: " + discount.code() + " total=" + totalAmount, null, discount.code());
    }
}
