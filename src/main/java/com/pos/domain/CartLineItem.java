package com.pos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CartLineItem {
    private final long productId;
    private final String productName;
    private final String sku;
    private int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal taxRate;
    private final boolean taxExempt;
    private final List<AppliedDiscount> lineDiscounts;

    public CartLineItem(long productId, String productName, String sku,
                        int quantity, BigDecimal unitPrice, BigDecimal taxRate,
                        boolean taxExempt) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.taxRate = taxRate;
        this.taxExempt = taxExempt;
        this.lineDiscounts = new ArrayList<>();
    }

    public BigDecimal getPreTaxTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getTaxAmount() {
        if (taxExempt) return BigDecimal.ZERO;
        return getPreTaxTotal().multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getLineDiscountTotal() {
        return lineDiscounts.stream()
            .map(AppliedDiscount::amountDeducted)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getLineTotal() {
        BigDecimal total = getPreTaxTotal().add(getTaxAmount()).subtract(getLineDiscountTotal());
        return total.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public void addDiscount(AppliedDiscount discount) { lineDiscounts.add(discount); }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public boolean isTaxExempt() { return taxExempt; }
    public List<AppliedDiscount> getLineDiscounts() { return List.copyOf(lineDiscounts); }
}
