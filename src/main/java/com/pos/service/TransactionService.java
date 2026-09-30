package com.pos.service;
import com.pos.domain.Cart;
import com.pos.domain.CartLineItem;
import com.pos.domain.CompletedTransaction;
import com.pos.dto.TransactionSummary;
import java.util.List;
public interface TransactionService {
    Cart getActiveCart(long sessionId);
    CartLineItem addItem(long sessionId, String sku, int qty);
    void removeItem(long sessionId, long productId);
    void updateQuantity(long sessionId, long productId, int qty);
    TransactionSummary calculateTotals(long sessionId);
    CompletedTransaction completeTransaction(long sessionId, List<com.pos.domain.PaymentEntry> payments);
    void voidTransaction(long sessionId);
}
