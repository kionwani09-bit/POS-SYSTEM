package com.pos.service.impl;

import com.pos.domain.AuditEventType;
import com.pos.domain.Product;
import com.pos.dto.ImportResult;
import com.pos.dto.ProductDto;
import com.pos.exception.ValidationException;
import com.pos.repository.InventoryRepository;
import com.pos.repository.PriceHistoryRepository;
import com.pos.repository.ProductRepository;
import com.pos.service.AuditService;
import com.pos.service.ProductService;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = Logger.getLogger(ProductServiceImpl.class.getName());

    private final ProductRepository productRepo;
    private final InventoryRepository inventoryRepo;
    private final PriceHistoryRepository priceHistoryRepo;
    private final AuditService auditService;
    private final long currentUserId; // injected from session context at call time

    public ProductServiceImpl(ProductRepository productRepo,
                               InventoryRepository inventoryRepo,
                               PriceHistoryRepository priceHistoryRepo,
                               AuditService auditService,
                               long currentUserId) {
        this.productRepo = productRepo;
        this.inventoryRepo = inventoryRepo;
        this.priceHistoryRepo = priceHistoryRepo;
        this.auditService = auditService;
        this.currentUserId = currentUserId;
    }

    @Override
    public Product createProduct(ProductDto dto) {
        if (dto.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit price must be non-negative.");
        }
        if (productRepo.findBySku(dto.sku()).isPresent()) {
            throw new ValidationException("SKU already exists: " + dto.sku());
        }
        Product p = new Product(0, dto.sku(), dto.name(), dto.description(),
            dto.unitPrice(), dto.taxCategoryId(), dto.taxExempt(), true,
            Instant.now(), Instant.now());
        long id = productRepo.save(p);

        // Create default inventory entry
        inventoryRepo.save(id, 0, 10);

        auditService.log(AuditEventType.PRODUCT_CREATED, currentUserId,
            "Created product: " + dto.sku() + " - " + dto.name(), null, dto.sku());
        return new Product(id, p.sku(), p.name(), p.description(), p.unitPrice(),
            p.taxCategoryId(), p.taxExempt(), true, p.createdAt(), p.updatedAt());
    }

    @Override
    public Product updateProduct(long productId, ProductDto dto) {
        Product existing = productRepo.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found: " + productId));

        if (dto.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit price must be non-negative.");
        }

        // Record price history if price changed
        if (existing.unitPrice().compareTo(dto.unitPrice()) != 0) {
            priceHistoryRepo.save(productId, existing.unitPrice(), dto.unitPrice(), currentUserId);
        }

        Product updated = new Product(productId, dto.sku(), dto.name(), dto.description(),
            dto.unitPrice(), dto.taxCategoryId(), dto.taxExempt(), existing.active(),
            existing.createdAt(), Instant.now());
        productRepo.update(updated);

        auditService.log(AuditEventType.PRODUCT_UPDATED, currentUserId,
            "Updated product: " + dto.sku(),
            "price=" + existing.unitPrice(), "price=" + dto.unitPrice());
        return updated;
    }

    @Override
    public void deleteProduct(long productId) {
        Product existing = productRepo.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found: " + productId));
        productRepo.softDelete(productId);
        auditService.log(AuditEventType.PRODUCT_DELETED, currentUserId,
            "Deleted (soft) product: " + existing.sku(), "active=true", "active=false");
    }

    @Override
    public List<Product> search(String query) {
        if (query == null || query.isBlank()) return productRepo.findAll();
        return productRepo.search(query.trim());
    }

    @Override
    public ImportResult importFromCsv(Path csvPath) {
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            int rowNum = 0;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (rowNum == 1 && line.toLowerCase().startsWith("sku")) continue; // skip header
                String[] parts = line.split(",", -1);
                if (parts.length < 6) {
                    errors.add("Row " + rowNum + ": expected 6 columns, got " + parts.length);
                    failureCount++;
                    continue;
                }
                try {
                    String sku = parts[0].trim();
                    String name = parts[1].trim();
                    String description = parts[2].trim();
                    BigDecimal price = new BigDecimal(parts[3].trim());
                    long taxCategoryId = Long.parseLong(parts[4].trim());
                    int qty = Integer.parseInt(parts[5].trim());

                    if (price.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException("negative price");

                    ProductDto dto = new ProductDto(sku, name, description, price, taxCategoryId, false);
                    Product product = createProduct(dto);
                    // Set initial inventory quantity from CSV
                    inventoryRepo.updateQuantity(product.id(), qty);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    failureCount++;
                    LOG.fine("CSV import skip row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }
        return new ImportResult(successCount, failureCount, errors);
    }
}
