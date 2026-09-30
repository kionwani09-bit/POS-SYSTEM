package com.pos.service.impl;

import com.pos.db.TransactionManager;
import com.pos.domain.*;
import com.pos.dto.*;
import com.pos.exception.ValidationException;
import com.pos.repository.*;
import com.pos.service.AuditService;
import com.pos.service.ReceiptService;
import com.pos.service.RefundService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class RefundServiceImpl implements RefundService {
    private static final Logger LOG = Logger.getLogger(RefundServiceImpl.class.getName());

    private final TransactionRepository transactionRepo;
    private final LineItemRepository lineItemRepo;
    private final InventoryRepository inventoryRepo;
    private final RefundRepository refundRepo;
    private final UserRepository userRepo;
    private final ReceiptService receiptService;
    private final AuditService auditService;
    private final TransactionManager txManager;

    public RefundServiceImpl(TransactionRepository transactionRepo,
                              LineItemRepository lineItemRepo,
                              InventoryRepository inventoryRepo,
                              RefundRepository refundRepo,
                              UserRepository userRepo,
                              ReceiptService receiptService,
                              AuditService auditService,
                              TransactionManager txManager) {
        this.transactionRepo = transactionRepo;
        this.lineItemRepo = lineItemRepo;
        this.inventoryRepo = inventoryRepo;
        this.refundRepo = refundRepo;
        this.userRepo = userRepo;
        this.receiptService = receiptService;
        this.auditService = auditService;
        this.txManager = txManager;
    }

    @Override
    public TransactionDetail lookupTransaction(String transactionId) {
        Map<String, Object> txnRow = transactionRepo.findByTransactionId(transactionId)
            .orElseThrow(() -> new ValidationException("Transaction not found: " + transactionId));

        long txnDbId = (long) txnRow.get("id");
        List<Map<String, Object>> lineRows = lineItemRepo.findByTransactionDbId(txnDbId);

        // Reconstruct CartLineItems from DB rows (tax rate unknown at this point — use ZERO)
        List<CartLineItem> lineItems = new ArrayList<>();
        for (Map<String, Object> row : lineRows) {
            CartLineItem li = new CartLineItem(
                (long) row.get("productId"),
                (String) row.get("name"),
                (String) row.get("sku"),
                (int) row.get("quantity"),
                (BigDecimal) row.get("unitPrice"),
                BigDecimal.ZERO,   // tax rate not needed for refund display
                false);
            lineItems.add(li);
        }

        // Reconstruct payments
        List<PaymentEntry> payments = new ArrayList<>();
        // (payment details not critical for lookup display — left minimal)

        return new TransactionDetail(
            (String) txnRow.get("transactionId"),
            (long) txnRow.get("cashierId"),
            lineItems,
            payments,
            (BigDecimal) txnRow.get("grandTotal"),
            (Instant) txnRow.get("createdAt"),
            TransactionStatus.valueOf((String) txnRow.get("status")));
    }

    @Override
    public RefundResult processRefund(RefundRequest request, long managerId) {
        // Validate manager role
        userRepo.findById(managerId)
            .filter(u -> u.role() == Role.MANAGER || u.role() == Role.ADMINISTRATOR)
            .orElseThrow(() -> new ValidationException("Manager credentials required for refunds."));

        // Lookup original transaction
        Map<String, Object> txnRow = transactionRepo.findByTransactionId(request.originalTransactionId())
            .orElseThrow(() -> new ValidationException(
                "Transaction not found: " + request.originalTransactionId()));

        long txnDbId = (long) txnRow.get("id");
        List<Map<String, Object>> lineRows = lineItemRepo.findByTransactionDbId(txnDbId);

        // Build map of lineItemId -> row for quick lookup
        Map<Long, Map<String, Object>> lineById = new LinkedHashMap<>();
        for (Map<String, Object> row : lineRows) {
            lineById.put((long) row.get("id"), row);
        }

        // Calculate refund amount for requested quantities
        BigDecimal refundTotal = BigDecimal.ZERO;
        Map<Long, Integer> toRefund = request.lineItemQuantities();

        for (Map.Entry<Long, Integer> entry : toRefund.entrySet()) {
            long lineItemId = entry.getKey();
            int refundQty = entry.getValue();
            Map<String, Object> row = lineById.get(lineItemId);
            if (row == null) throw new ValidationException("Line item not found: " + lineItemId);
            int origQty = (int) row.get("quantity");
            if (refundQty > origQty) {
                throw new ValidationException(
                    "Refund qty " + refundQty + " exceeds original qty " + origQty);
            }
            BigDecimal unitTotal = (BigDecimal) row.get("lineTotal");
            BigDecimal perUnit = unitTotal.divide(BigDecimal.valueOf(origQty), 2, RoundingMode.HALF_UP);
            refundTotal = refundTotal.add(perUnit.multiply(BigDecimal.valueOf(refundQty)));
        }
        refundTotal = refundTotal.setScale(2, RoundingMode.HALF_UP);

        String paymentMethod = request.paymentMethod() != null
            ? request.paymentMethod() : "CASH";
        String refundId = "RFN-" + System.currentTimeMillis();
        final BigDecimal finalRefundTotal = refundTotal;

        txManager.executeInTransaction(conn -> {
            // 1. Insert refund header
            long refundDbId = refundRepo.saveInTx(
                conn, refundId, txnDbId, managerId, finalRefundTotal, paymentMethod);

            // 2. Insert refund line items + restore inventory
            for (Map.Entry<Long, Integer> entry : toRefund.entrySet()) {
                long lineItemId = entry.getKey();
                int refundQty = entry.getValue();
                Map<String, Object> row = lineById.get(lineItemId);

                int origQty = (int) row.get("quantity");
                BigDecimal unitTotal = (BigDecimal) row.get("lineTotal");
                BigDecimal perUnit = unitTotal.divide(BigDecimal.valueOf(origQty), 2, RoundingMode.HALF_UP);
                BigDecimal refundedAmount = perUnit.multiply(BigDecimal.valueOf(refundQty));

                refundRepo.saveLineItemInTx(conn, refundDbId, lineItemId, refundQty, refundedAmount);

                // Restore inventory
                long productId = (long) row.get("productId");
                Inventory inv = inventoryRepo.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Inventory not found for productId: " + productId));
                inventoryRepo.updateQuantityInTx(conn, productId, inv.quantity() + refundQty);
            }
            return null;
        });

        // Audit log
        auditService.log(AuditEventType.REFUND_COMPLETED, managerId,
            "Refund processed: " + refundId + " amount=" + refundTotal
                + " originalTxn=" + request.originalTransactionId(),
            null, refundId);

        // Generate refund receipt (best-effort print)
        try {
            Receipt originalReceipt = receiptService.getReceiptByTransactionId(
                request.originalTransactionId());
            String refundContent = "=== REFUND RECEIPT ===\n"
                + "Refund ID: " + refundId + "\n"
                + "Original Txn: " + request.originalTransactionId() + "\n"
                + "Amount Refunded: " + refundTotal + "\n"
                + "Method: " + paymentMethod + "\n"
                + "======================\n";
            Receipt refundReceipt = new Receipt(0, refundId, refundContent, false, Instant.now());
            receiptService.printReceipt(refundReceipt);
        } catch (Exception e) {
            LOG.warning("Refund receipt print failed: " + e.getMessage());
        }

        return new RefundResult(refundId, refundTotal, paymentMethod);
    }
}
