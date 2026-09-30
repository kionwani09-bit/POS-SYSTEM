package com.pos.service.impl;

import com.pos.domain.AuditEventType;
import com.pos.domain.Inventory;
import com.pos.domain.Product;
import com.pos.exception.ValidationException;
import com.pos.repository.InventoryAdjustmentRepository;
import com.pos.repository.InventoryRepository;
import com.pos.repository.ProductRepository;
import com.pos.service.AuditService;
import com.pos.service.InventoryService;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepo;
    private final InventoryAdjustmentRepository adjustmentRepo;
    private final ProductRepository productRepo;
    private final AuditService auditService;

    public InventoryServiceImpl(InventoryRepository inventoryRepo,
                                 InventoryAdjustmentRepository adjustmentRepo,
                                 ProductRepository productRepo,
                                 AuditService auditService) {
        this.inventoryRepo = inventoryRepo;
        this.adjustmentRepo = adjustmentRepo;
        this.productRepo = productRepo;
        this.auditService = auditService;
    }

    @Override
    public void adjustInventory(long productId, int delta, String reason, long userId) {
        Inventory current = inventoryRepo.findByProductId(productId)
            .orElseThrow(() -> new ValidationException("Inventory not found for product: " + productId));

        int newQty = current.quantity() + delta;
        if (newQty < 0) {
            throw new ValidationException("Adjustment would result in negative inventory.");
        }

        inventoryRepo.updateQuantity(productId, newQty);
        adjustmentRepo.save(productId, current.quantity(), newQty, reason, userId);

        auditService.log(AuditEventType.INVENTORY_ADJUSTED, userId,
            "Inventory adjusted for productId=" + productId + " reason=" + reason,
            String.valueOf(current.quantity()), String.valueOf(newQty));
    }

    @Override
    public List<Product> getLowStockProducts() {
        List<Inventory> lowStock = inventoryRepo.findLowStock();
        return lowStock.stream()
            .map(inv -> productRepo.findById(inv.productId()).orElse(null))
            .filter(p -> p != null && p.active())
            .collect(Collectors.toList());
    }

    @Override
    public List<Inventory> generateInventoryReport() {
        return inventoryRepo.findAll();
    }
}
