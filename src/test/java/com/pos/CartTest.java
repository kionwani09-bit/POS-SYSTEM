package com.pos;

import com.pos.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    @Test
    void totalsAreCalculatedAndDiscountsClamped() {
        Cart cart = new Cart(1L);
        CartLineItem item = new CartLineItem(1L, "Widget", "SKU-1", 2,
            new BigDecimal("10.00"), new BigDecimal("0.10"), false);
        cart.addLineItem(item);

        assertThat(cart.getSubtotal()).isEqualByComparingTo("20.00");
        assertThat(cart.getTotalTax()).isEqualByComparingTo("2.00");
        assertThat(cart.getGrandTotal()).isEqualByComparingTo("22.00");

        cart.addTransactionDiscount(new AppliedDiscount(9L, "VIP", DiscountType.PERCENTAGE,
            DiscountScope.TRANSACTION, new BigDecimal("50.00")));
        assertThat(cart.getGrandTotal()).isEqualByComparingTo("0.00");

        cart.removeLineItem(1L);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void removingAtZeroQuantityRemovesLine() {
        Cart cart = new Cart(2L);
        CartLineItem item = new CartLineItem(2L, "Widget", "SKU-2", 1,
            new BigDecimal("15.00"), BigDecimal.ZERO, true);
        cart.addLineItem(item);

        item.setQuantity(0);
        cart.removeLineItem(2L);

        assertThat(cart.getLineItems()).isEmpty();
    }
}
