package com.pos.service.impl;

import com.pos.domain.GiftCard;
import com.pos.domain.PaymentEntry;
import com.pos.domain.PaymentMethod;
import com.pos.exception.PaymentDeclinedException;
import com.pos.exception.ValidationException;
import com.pos.hardware.PaymentGateway;
import com.pos.repository.GiftCardRepository;
import com.pos.service.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentServiceImpl implements PaymentService {

    private final PaymentGateway gateway;
    private final GiftCardRepository giftCardRepo;

    public PaymentServiceImpl(PaymentGateway gateway, GiftCardRepository giftCardRepo) {
        this.gateway = gateway;
        this.giftCardRepo = giftCardRepo;
    }

    /**
     * Processes one payment entry against a remaining total.
     * Returns the change due (only meaningful for CASH; zero for card/gift card).
     * For GIFT_CARD with insufficient balance, returns the remaining amount still owed
     * as a negative value so the caller knows how much more to collect.
     */
    @Override
    public BigDecimal processPayment(PaymentEntry entry, BigDecimal remainingTotal) {
        return switch (entry.method()) {
            case CASH -> processCash(entry, remainingTotal);
            case CREDIT_CARD, DEBIT_CARD -> processCard(entry);
            case GIFT_CARD -> processGiftCard(entry, remainingTotal);
        };
    }

    @Override
    public BigDecimal calculateChange(BigDecimal tendered, BigDecimal grandTotal) {
        if (tendered == null || grandTotal == null) return BigDecimal.ZERO;
        return tendered.subtract(grandTotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private BigDecimal processCash(PaymentEntry entry, BigDecimal remainingTotal) {
        if (entry.amount().compareTo(remainingTotal) < 0) {
            throw new ValidationException(
                "Insufficient cash tendered: need " + remainingTotal
                + " but received " + entry.amount());
        }
        return calculateChange(entry.amount(), remainingTotal);
    }

    private BigDecimal processCard(PaymentEntry entry) {
        PaymentGateway.GatewayResponse response = gateway.authorize(
            new PaymentGateway.CardPaymentRequest(
                entry.reference(), entry.amount(), entry.method().name()));

        if (!response.approved()) {
            throw new PaymentDeclinedException(response.message());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal processGiftCard(PaymentEntry entry, BigDecimal remainingTotal) {
        GiftCard card = giftCardRepo.findByCardNumber(entry.reference())
            .orElseThrow(() -> new ValidationException(
                "Gift card not found: " + entry.reference()));

        if (card.balance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Gift card has no balance: " + entry.reference());
        }

        BigDecimal applied = card.balance().min(remainingTotal);
        BigDecimal newBalance = card.balance().subtract(applied).setScale(2, RoundingMode.HALF_UP);
        giftCardRepo.updateBalance(card.cardNumber(), newBalance);

        // Return remaining amount still owed (0 if card covered it all)
        return remainingTotal.subtract(applied).setScale(2, RoundingMode.HALF_UP);
    }
}
