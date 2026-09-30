package com.pos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Cart {
    private final long sessionId;
    private final List<CartLineItem> lineItems = new ArrayList<>();
    private final List<AppliedDiscount> transactionDiscounts = new ArrayList<>();

    public Cart(long sessionId) {
        this.sessionId = sessionId;
    }

    public void addLineItem(CartLineItem item) { lineItems.add(item); }

    public void removeLineItem(long productId) {
        lineItems.removeIf(i -> i.getProductId() == productId);
    }

    public void addTransactionDiscount(AppliedDiscount discount) {
        transactionDiscounts.add(discount);
    }

    public BigDecimal getSubtotal() {
        return lineItems.stream()
            .map(CartLineItem::getPreTaxTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalTax() {
        return lineItems.stream()
            .map(CartLineItem::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalLineDiscounts() {
        return lineItems.stream()
            .map(CartLineItem::getLineDiscountTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalTransactionDiscounts() {
        return transactionDiscounts.stream()
            .map(AppliedDiscount::amountDeducted)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDiscount() {
        return getTotalLineDiscounts().add(getTotalTransactionDiscounts())
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getGrandTotal() {
        BigDecimal raw = getSubtotal().add(getTotalTax()).subtract(getTotalDiscount());
        return raw.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isEmpty() { return lineItems.isEmpty(); }
    public long getSessionId() { return sessionId; }
    public List<CartLineItem> getLineItems() { return List.copyOf(lineItems); }
    public List<AppliedDiscount> getTransactionDiscounts() { return List.copyOf(transactionDiscounts); }
    public void clear() { lineItems.clear(); transactionDiscounts.clear(); }
}
