package com.pos.service;
import com.pos.domain.CartLineItem;
import com.pos.domain.TaxCategory;
import java.math.BigDecimal;
import java.util.List;
public interface TaxService {
    BigDecimal calculateLineTax(CartLineItem item);
    BigDecimal calculateTransactionTax(List<CartLineItem> items);
    List<TaxCategory> getAllCategories();
}
