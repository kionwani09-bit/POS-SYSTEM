package com.pos.service;
import com.pos.domain.PaymentEntry;
import java.math.BigDecimal;
public interface PaymentService {
    BigDecimal processPayment(PaymentEntry entry, BigDecimal remainingTotal);
    BigDecimal calculateChange(BigDecimal tendered, BigDecimal grandTotal);
}
