package com.pos.service;
import com.pos.dto.RefundRequest;
import com.pos.dto.RefundResult;
import com.pos.dto.TransactionDetail;
public interface RefundService {
    TransactionDetail lookupTransaction(String transactionId);
    RefundResult processRefund(RefundRequest request, long managerId);
}
