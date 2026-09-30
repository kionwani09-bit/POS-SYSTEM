package com.pos.service;
import com.pos.domain.CompletedTransaction;
import com.pos.dto.Receipt;
public interface ReceiptService {
    Receipt generateReceipt(CompletedTransaction txn);
    void printReceipt(Receipt receipt);
    void emailReceipt(Receipt receipt, String emailAddress);
    Receipt getReceiptByTransactionId(String transactionId);
}
