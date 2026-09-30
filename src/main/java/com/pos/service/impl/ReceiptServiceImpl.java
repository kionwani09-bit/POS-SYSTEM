package com.pos.service.impl;

import com.pos.domain.AppliedDiscount;
import com.pos.domain.CartLineItem;
import com.pos.domain.CompletedTransaction;
import com.pos.domain.PaymentEntry;
import com.pos.dto.Receipt;
import com.pos.exception.PrinterUnavailableException;
import com.pos.hardware.ReceiptPrinter;
import com.pos.repository.ReceiptRepository;
import com.pos.service.ConfigService;
import com.pos.service.ReceiptService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class ReceiptServiceImpl implements ReceiptService {
    private static final Logger LOG = Logger.getLogger(ReceiptServiceImpl.class.getName());
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ReceiptRepository receiptRepo;
    private final ReceiptPrinter printer;
    private final ConfigService configService;

    public ReceiptServiceImpl(ReceiptRepository receiptRepo,
                               ReceiptPrinter printer,
                               ConfigService configService) {
        this.receiptRepo = receiptRepo;
        this.printer = printer;
        this.configService = configService;
    }

    @Override
    public Receipt generateReceipt(CompletedTransaction txn) {
        String storeName    = configService.get("store.name",    "POS Store");
        String storeAddress = configService.get("store.address", "");
        String header       = configService.get("receipt.header", "Thank you for shopping!");
        String footer       = configService.get("receipt.footer", "Please come again.");

        StringBuilder sb = new StringBuilder();
        line(sb, "=", 40);
        center(sb, storeName, 40);
        if (!storeAddress.isBlank()) center(sb, storeAddress, 40);
        line(sb, "=", 40);
        sb.append("Transaction: ").append(txn.transactionId()).append("\n");
        sb.append("Date:        ").append(DATE_FMT.format(txn.completedAt())).append("\n");
        sb.append("Cashier ID:  ").append(txn.cashierId()).append("\n");
        line(sb, "-", 40);

        // Line items
        for (CartLineItem item : txn.lineItems()) {
            sb.append(String.format("%-20s %3d x %6.2f%n",
                truncate(item.getProductName(), 20),
                item.getQuantity(), item.getUnitPrice()));
            if (!item.isTaxExempt()) {
                sb.append(String.format("  Tax:                     %6.2f%n", item.getTaxAmount()));
            }
            if (!item.getLineDiscounts().isEmpty()) {
                for (AppliedDiscount d : item.getLineDiscounts()) {
                    sb.append(String.format("  Disc %-15s  -%6.2f%n",
                        truncate(d.name(), 15), d.amountDeducted()));
                }
            }
            sb.append(String.format("  Line Total:              %6.2f%n", item.getLineTotal()));
        }

        line(sb, "-", 40);

        // Transaction-level discounts
        for (AppliedDiscount d : txn.appliedDiscounts()) {
            if (d.scope() == com.pos.domain.DiscountScope.TRANSACTION) {
                sb.append(String.format("Discount %-16s -%6.2f%n",
                    truncate(d.name(), 16), d.amountDeducted()));
            }
        }

        sb.append(String.format("Subtotal:                %7.2f%n", txn.subtotal()));
        sb.append(String.format("Tax:                     %7.2f%n", txn.totalTax()));
        sb.append(String.format("Discount:               -%7.2f%n", txn.totalDiscount()));
        line(sb, "-", 40);
        sb.append(String.format("TOTAL:                   %7.2f%n", txn.grandTotal()));
        line(sb, "-", 40);

        // Payments
        for (PaymentEntry p : txn.payments()) {
            sb.append(String.format("%-20s     %6.2f%n", p.method().name(), p.amount()));
        }

        line(sb, "=", 40);
        center(sb, header, 40);
        center(sb, footer, 40);
        line(sb, "=", 40);

        return new Receipt(0, txn.transactionId(), sb.toString(), false, txn.completedAt());
    }

    @Override
    public void printReceipt(Receipt receipt) {
        if (!printer.isAvailable()) {
            LOG.warning("Printer unavailable — receipt saved to DB for retry.");
            throw new PrinterUnavailableException();
        }
        ReceiptPrinter.PrintResult result = printer.print(receipt);
        if (!result.success()) {
            throw new PrinterUnavailableException();
        }
        // Mark printed in DB if the receipt has a real DB id
        if (receipt.id() > 0) {
            receiptRepo.markPrinted(receipt.id());
        }
    }

    @Override
    public void emailReceipt(Receipt receipt, String emailAddress) {
        // Stub — log the email send; replace with JavaMail in production
        LOG.info("EMAIL RECEIPT to " + emailAddress + ":\n" + receipt.content());
    }

    @Override
    public Receipt getReceiptByTransactionId(String transactionId) {
        return receiptRepo.findByTransactionId(transactionId)
            .orElseThrow(() -> new com.pos.exception.ValidationException(
                "Receipt not found for transaction: " + transactionId));
    }

    // ── formatting helpers ─────────────────────────────────────────────────

    private void line(StringBuilder sb, String ch, int width) {
        sb.append(ch.repeat(width)).append("\n");
    }

    private void center(StringBuilder sb, String text, int width) {
        if (text == null || text.isBlank()) return;
        int pad = Math.max(0, (width - text.length()) / 2);
        sb.append(" ".repeat(pad)).append(text).append("\n");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
