package com.pos.service;
import com.pos.domain.Product;
import com.pos.dto.ImportResult;
import com.pos.dto.ProductDto;
import java.nio.file.Path;
import java.util.List;
public interface ProductService {
    Product createProduct(ProductDto dto);
    Product updateProduct(long productId, ProductDto dto);
    void deleteProduct(long productId);
    List<Product> search(String query);
    ImportResult importFromCsv(Path csvPath);
}
