package com.pos.service;
import com.pos.domain.Product;
import java.util.List;
public interface InventoryService {
    void adjustInventory(long productId, int delta, String reason, long userId);
    List<Product> getLowStockProducts();
    List<com.pos.domain.Inventory> generateInventoryReport();
}
