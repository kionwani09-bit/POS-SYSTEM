package com.pos.hardware;

import com.pos.dto.Receipt;

import java.util.ArrayList;
import java.util.List;

public class MockReceiptPrinter implements ReceiptPrinter {
    private final List<String> printed = new ArrayList<>();
    private boolean available = true;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<String> getPrinted() {
        return List.copyOf(printed);
    }

    @Override
    public PrintResult print(Receipt receipt) {
        if (!available) {
            return new PrintResult(false, "Printer unavailable");
        }
        printed.add(receipt.content());
        return new PrintResult(true, "Printed");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
