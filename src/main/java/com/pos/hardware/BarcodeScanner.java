package com.pos.hardware;

import java.util.function.Consumer;

public interface BarcodeScanner {
    void startListening(Consumer<String> onSkuScanned);
    void stopListening();
}
