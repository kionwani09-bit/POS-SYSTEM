package com.pos.hardware;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MockBarcodeScanner implements BarcodeScanner {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scanTask;
    private Consumer<String> listener;

    @Override
    public void startListening(Consumer<String> onSkuScanned) {
        this.listener = onSkuScanned;
        if (scanTask != null && !scanTask.isCancelled()) {
            scanTask.cancel(false);
        }
        scanTask = scheduler.scheduleAtFixedRate(() -> {
            if (listener != null) {
                listener.accept("SKU-TEST-" + System.currentTimeMillis());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void stopListening() {
        if (scanTask != null) {
            scanTask.cancel(true);
        }
        listener = null;
    }
}
