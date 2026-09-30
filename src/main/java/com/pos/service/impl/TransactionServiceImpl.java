package com.pos.service.impl;

import com.pos.db.TransactionManager;
import com.pos.domain.*;
import com.pos.dto.TransactionSummary;
import com.pos.exception.OutOfStockException;
import com.pos.exception.ValidationException;
import com.pos.repository.*;
import com.pos.service.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TransactionServiceImpl implements TransactionService {
    private static final Logger LOG = Logger.getLogger(TransactionServiceImpl.class.getName());

    private final Map<Long, Cart> activeCarts = new ConcurrentHashMap<>();

    private final ProductRepository productRepo;
    private final InventoryRepository inventoryRepo;
    private final TaxCategoryRepository taxCategoryRepo;
    private final TransactionRepository transactionRepo;
    private final LineItemRepository lineItemRepo;
    private final PaymentRepository paymentRepo;
    private final AppliedDiscountRepository appliedDiscountRepo;
    private final ReceiptRepository receiptRepo;
    private final TransactionManager txManager;
    private final DiscountServiceImpl discountService;
    private final ReceiptService receiptService;
    private final AuditService auditService;
    private final long currentUserId;
    private final long currentShiftId;

    public TransactionServiceImpl(ProductRepository productRepo,
                                   InventoryRepository inventoryRepo,
                                   TaxCategoryRepository taxCategoryRepo,
                                   TransactionRepository transactionRepo,
                                   LineItemRepository lineItemRepo,
                                   PaymentRepository paymentRepo,
                                   AppliedDiscountRepository appliedDiscountRepo,
                                   ReceiptRepository receiptRepo,
                                   TransactionManager txManager,
                                   DiscountServiceImpl discountService,
                                   ReceiptService receiptService,
                                   AuditService auditService,
                                   long currentUserId,
                                   long currentShiftId) {
        this.productRepo = productRepo;
        this.inventoryRepo = inventoryRepo;
        this.taxCategoryRepo = taxCategoryRepo;
        this.transactionRepo = transactionRepo;
        this.lineItemRepo = lineItemRepo;
        this.paymentRepo = paymentRepo;
        this.appliedDiscountRepo = appliedDiscountRepo;
        this.receiptRepo = receiptRepo;
        this.txManager = txManager;
        this.discountService = discountService;
        this.receiptService = receiptService;
        this.auditService = auditService;
        this.currentUserId = currentUserId;
        this.currentShiftId = currentShiftId;
    }

    @Override
    public Cart getActiveCart(long sessionId) {
        return activeCarts.computeIfAbsent(sessionId, Cart::new);
    }

    @Override
    public CartLineItem addItem(long sessionId, String sku, int qty) {
        Product product = productRepo.findBySku(sku)
            .orElseThrow(() -> new ValidationException("Product not found: " + sku));

        Inventory inventory = inventoryRepo.findByProductId(product.id())
            .orElseThrow(() -> new ValidationException("Inventory not found for: " + sku));

        if (inventory.quantity() <= 0) {
            throw new OutOfStockException(sku);
        }
        if (qty > inventory.quantity()) {
            throw new OutOfStockException(sku + " (requested " + qty + ", available " + inventory.quantity() + ")");
        }

        // Resolve tax rate
        BigDecimal taxRate = BigDecimal.ZERO;
        if (!product.taxExempt() && product.taxCategoryId() > 0) {
            taxRate = taxCategoryRepo.findById(product.taxCategoryId())
                .map(TaxCategory::rate)
                .orElse(BigDecimal.ZERO);
        }

        Cart cart = getActiveCart(sessionId);

        // If product already in cart, increment quantity
        Optional<CartLineItem> existing = cart.getLineItems().stream()
            .filter(li -> li.getProductId() == product.id())
            .findFirst();

        CartLineItem lineItem;
        if (existing.isPresent()) {
            // Mutable update — get from live list
            CartLineItem live = cart.getLineItems().stream()
                .filter(li -> li.getProductId() == product.id())
                .findFirst().get();
            live.setQuantity(live.getQuantity() + qty);
            lineItem = live;
        } else {
            lineItem = new CartLineItem(
                product.id(), product.name(), product.sku(),
                qty, product.unitPrice(), taxRate, product.taxExempt());
            cart.addLineItem(lineItem);
            // Auto-apply active promotions to the new line item
            discountService.autoApplyPromotions(cart, lineItem);
        }

        return lineItem;
    }

    @Override
    public void removeItem(long sessionId, long productId) {
        getActiveCart(sessionId).removeLineItem(productId);
    }

    @Override
    public void updateQuantity(long sessionId, long productId, int qty) {
        Cart cart = getActiveCart(sessionId);
        if (qty <= 0) {
            cart.removeLineItem(productId);
            return;
        }
        cart.getLineItems().stream()
            .filter(li -> li.getProductId() == productId)
            .findFirst()
            .ifPresent(li -> li.setQuantity(qty));
    }

    @Override
    public TransactionSummary calculateTotals(long sessionId) {
        Cart cart = getActiveCart(sessionId);
        return new TransactionSummary(
            cart.getSubtotal(), cart.getTotalTax(),
            cart.getTotalDiscount(), cart.getGrandTotal());
    }

    @Override
    public CompletedTransaction completeTransaction(long sessionId, List<PaymentEntry> payments) {
        Cart cart = getActiveCart(sessionId);

        if (cart.isEmpty()) {
            throw new ValidationException("Cannot complete an empty transaction.");
        }

        // Validate payment sum covers grand total
        BigDecimal grandTotal = cart.getGrandTotal();
        BigDecimal totalPaid = payments.stream()
            .map(PaymentEntry::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPaid.compareTo(grandTotal) < 0) {
            throw new ValidationException(
                "Insufficient payment: need " + grandTotal + " but got " + totalPaid);
        }

        String transactionId = "TXN-" + System.currentTimeMillis();
        List<CartLineItem> lineItems = new ArrayList<>(cart.getLineItems());
        List<AppliedDiscount> allDiscounts = new ArrayList<>();
        lineItems.forEach(li -> allDiscounts.addAll(li.getLineDiscounts()));
        allDiscounts.addAll(cart.getTransactionDiscounts());

        CompletedTransaction completed = txManager.executeInTransaction(conn -> {
            // 1. Insert transaction header
            long txnDbId = transactionRepo.saveInTx(conn, transactionId, sessionId,
                currentUserId, currentShiftId,
                cart.getSubtotal(), cart.getTotalTax(),
                cart.getTotalDiscount(), grandTotal,
                TransactionStatus.COMPLETED);

            // 2. Insert line items
            lineItemRepo.saveAllInTx(conn, txnDbId, lineItems);

            // 3. Insert payments
            paymentRepo.saveAllInTx(conn, txnDbId, payments);

            // 4. Decrement inventory for each line item
            for (CartLineItem item : lineItems) {
                Inventory inv = inventoryRepo.findByProductId(item.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                        "Inventory not found during commit for: " + item.getSku()));
                int newQty = inv.quantity() - item.getQuantity();
                inventoryRepo.updateQuantityInTx(conn, item.getProductId(), newQty);
            }

            // 5. Insert applied discounts
            if (!allDiscounts.isEmpty()) {
                appliedDiscountRepo.saveAllInTx(conn, txnDbId, allDiscounts);
            }

            // 6. Generate and save receipt
            CompletedTransaction txn = new CompletedTransaction(
                transactionId, currentUserId, sessionId, currentShiftId,
                lineItems, payments, allDiscounts,
                cart.getSubtotal(), cart.getTotalTax(),
                cart.getTotalDiscount(), grandTotal,
                Instant.now(), TransactionStatus.COMPLETED);

            com.pos.dto.Receipt receipt = receiptService.generateReceipt(txn);
            receiptRepo.saveInTx(conn, txnDbId, receipt.content());

            return txn;
        });

        // 7. Audit log (outside JDBC transaction — non-critical)
        auditService.log(AuditEventType.TRANSACTION_COMPLETED, currentUserId,
            "Transaction completed: " + transactionId + " total=" + grandTotal, null, transactionId);

        // 8. Clear cart
        cart.clear();
        activeCarts.remove(sessionId);

        // 9. Print receipt asynchronously (best-effort)
        try {
            com.pos.dto.Receipt receipt = receiptService.getReceiptByTransactionId(transactionId);
            receiptService.printReceipt(receipt);
        } catch (Exception e) {
            LOG.warning("Receipt print failed (saved to DB for retry): " + e.getMessage());
        }

        return completed;
    }

    @Override
    public void voidTransaction(long sessionId) {
        Cart cart = getActiveCart(sessionId);
        cart.clear();
        activeCarts.remove(sessionId);
        auditService.log(AuditEventType.TRANSACTION_VOIDED, currentUserId,
            "Transaction voided for sessionId=" + sessionId, null, null);
    }
}
