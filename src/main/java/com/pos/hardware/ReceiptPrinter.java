package com.pos.hardware;

import com.pos.dto.Receipt;

public interface ReceiptPrinter {
    PrintResult print(Receipt receipt);
    boolean isAvailable();

    record PrintResult(boolean success, String message) {}
}
