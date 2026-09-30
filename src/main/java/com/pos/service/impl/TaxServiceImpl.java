package com.pos.service.impl;

import com.pos.domain.CartLineItem;
import com.pos.domain.TaxCategory;
import com.pos.repository.TaxCategoryRepository;
import com.pos.service.TaxService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TaxServiceImpl implements TaxService {

    private final TaxCategoryRepository taxCategoryRepo;

    public TaxServiceImpl(TaxCategoryRepository taxCategoryRepo) {
        this.taxCategoryRepo = taxCategoryRepo;
    }

    @Override
    public BigDecimal calculateLineTax(CartLineItem item) {
        if (item.isTaxExempt()) return BigDecimal.ZERO;
        // taxRate is already stored on the CartLineItem from the tax category at addItem time
        return item.getPreTaxTotal()
            .multiply(item.getTaxRate())
            .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTransactionTax(List<CartLineItem> items) {
        return items.stream()
            .map(this::calculateLineTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<TaxCategory> getAllCategories() {
        return taxCategoryRepo.findAll();
    }
}
