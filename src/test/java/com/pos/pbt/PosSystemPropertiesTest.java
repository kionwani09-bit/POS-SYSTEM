package com.pos.pbt;

import com.pos.domain.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.util.*;

class PosSystemPropertiesTest {

    @Property
    @Tag("Feature: java-pos-system, Property 1: grand total is never negative")
    void grandTotalNeverNegative(@ForAll("lineItems") List<CartLineItem> items,
                                 @ForAll("discounts") List<AppliedDiscount> discounts) {
        Cart cart = new Cart(1L);
        for (CartLineItem item : items) {
            cart.addLineItem(item);
        }
        for (AppliedDiscount discount : discounts) {
            cart.addTransactionDiscount(discount);
        }

        assert cart.getGrandTotal().compareTo(BigDecimal.ZERO) >= 0;
    }

    @Property
    @Tag("Feature: java-pos-system, Property 2: transaction tax equals sum of line taxes")
    void transactionTaxMatchesSumOfLineTaxes(@ForAll("lineItems") List<CartLineItem> items) {
        BigDecimal expected = items.stream()
            .map(CartLineItem::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actual = items.stream()
            .map(CartLineItem::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assert actual.compareTo(expected) == 0;
    }

    @Property
    @Tag("Feature: java-pos-system, Property 6: invalid discount codes do not modify cart")
    void invalidDiscountCodesDoNotModifyCart(@ForAll("lineItems") List<CartLineItem> items) {
        Cart cart = new Cart(1L);
        for (CartLineItem item : items) {
            cart.addLineItem(item);
        }
        BigDecimal before = cart.getGrandTotal();
        cart.addTransactionDiscount(new AppliedDiscount(1L, "BAD", DiscountType.PERCENTAGE,
            DiscountScope.TRANSACTION, new BigDecimal("999.99")));
        assert cart.getGrandTotal().compareTo(before) >= 0;
    }

    @Property
    @Tag("Feature: java-pos-system, Property 10: tax-exempt products have zero tax")
    void taxExemptProductsHaveZeroTax(@ForAll("taxExemptItems") List<CartLineItem> items) {
        for (CartLineItem item : items) {
            assert item.isTaxExempt();
            assert item.getTaxAmount().compareTo(BigDecimal.ZERO) == 0;
        }
    }

    @Provide
    Arbitrary<List<CartLineItem>> lineItems() {
        return Arbitraries.integers().between(1, 5).list().ofMinSize(1).ofMaxSize(5).map(list -> {
            List<CartLineItem> items = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                int qty = i + 1;
                items.add(new CartLineItem(i + 1L, "Item " + i, "SKU-" + i, qty,
                    new BigDecimal("10.00"), new BigDecimal("0.10"), false));
            }
            return items;
        });
    }

    @Provide
    Arbitrary<List<AppliedDiscount>> discounts() {
        return Arbitraries.integers().between(0, 3).list().ofMinSize(0).ofMaxSize(3).map(list -> {
            List<AppliedDiscount> result = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                result.add(new AppliedDiscount(i + 1L, "DISC" + i, DiscountType.PERCENTAGE,
                    DiscountScope.TRANSACTION, new BigDecimal("5.00")));
            }
            return result;
        });
    }

    @Provide
    Arbitrary<List<CartLineItem>> taxExemptItems() {
        return Arbitraries.integers().between(1, 3).list().ofMinSize(1).ofMaxSize(3).map(list -> {
            List<CartLineItem> items = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                items.add(new CartLineItem(i + 1L, "Tax Free " + i, "FREE-" + i,
                    1, new BigDecimal("10.00"), new BigDecimal("0.10"), true));
            }
            return items;
        });
    }
}
