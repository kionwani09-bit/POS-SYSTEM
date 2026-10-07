package com.pos.ui;

import com.pos.domain.*;
import com.pos.dto.TransactionSummary;
import com.pos.exception.OutOfStockException;
import com.pos.exception.ValidationException;
import com.pos.hardware.BarcodeScanner;
import com.pos.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CartPanel extends JPanel {
    private final TransactionService transactionService;
    private final BarcodeScanner barcodeScanner;
    private final JTextField skuField = new JTextField(18);
    private final JTextField qtyField = new JTextField("1", 5);
    private final JButton addButton = new JButton("Add Item");
    private final JButton removeButton = new JButton("Remove Item");
    private final JButton updateQtyButton = new JButton("Update Qty");
    private final JButton discountButton = new JButton("Apply Discount");
    private final JButton voidButton = new JButton("Void Transaction");
    private final JButton payButton = new JButton("Pay");
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Product", "Qty", "Unit Price", "Discounts", "Line Total"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JLabel subtotalLabel = new JLabel("0.00");
    private final JLabel taxLabel = new JLabel("0.00");
    private final JLabel discountLabel = new JLabel("0.00");
    private final JLabel totalLabel = new JLabel("0.00");

    public CartPanel(TransactionService transactionService, BarcodeScanner barcodeScanner) {
        this.transactionService = transactionService;
        this.barcodeScanner = barcodeScanner;
        buildUi();
        bindScanner();
        refreshCart();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("SKU:"));
        top.add(skuField);
        top.add(new JLabel("Qty:"));
        top.add(qtyField);
        top.add(addButton);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(removeButton);
        actions.add(updateQtyButton);
        actions.add(discountButton);
        actions.add(voidButton);
        actions.add(payButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0;
        footer.add(new JLabel("Subtotal:"), gbc);
        gbc.gridx = 1; footer.add(subtotalLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        footer.add(new JLabel("Tax:"), gbc);
        gbc.gridx = 1; footer.add(taxLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        footer.add(new JLabel("Discounts:"), gbc);
        gbc.gridx = 1; footer.add(discountLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        footer.add(new JLabel("Grand Total:"), gbc);
        gbc.gridx = 1; footer.add(totalLabel, gbc);

        add(footer, BorderLayout.SOUTH);
        add(actions, BorderLayout.EAST);

        addButton.addActionListener(e -> addItem());
        removeButton.addActionListener(e -> removeSelectedItem());
        updateQtyButton.addActionListener(e -> updateSelectedQuantity());
        discountButton.addActionListener(e -> applyDiscount());
        voidButton.addActionListener(e -> voidTransaction());
        payButton.addActionListener(e -> openPaymentDialog());
    }

    private void bindScanner() {
        if (barcodeScanner != null) {
            barcodeScanner.startListening(sku -> {
                SwingUtilities.invokeLater(() -> {
                    skuField.setText(sku);
                    addItem();
                });
            });
        }
    }

    private void addItem() {
        String sku = skuField.getText() == null ? "" : skuField.getText().trim();
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a SKU or scan an item.");
            return;
        }

        try {
            int qty = parseQty();
            transactionService.addItem(1L, sku, qty);
            clearInputs();
            refreshCart();
        } catch (OutOfStockException | ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cart Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedItem() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to remove.");
            return;
        }
        String sku = String.valueOf(tableModel.getValueAt(row, 0));
        // lookup by selected product name is intentionally lightweight for UI scaffolding
        refreshCart();
    }

    private void updateSelectedQuantity() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to update.");
            return;
        }
        try {
            int qty = parseQty();
            // quantity update uses selected row; placeholder implementation for UI scaffold
            refreshCart();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid quantity.");
        }
    }

    private void applyDiscount() {
        String code = JOptionPane.showInputDialog(this, "Enter discount code:");
        if (code != null && !code.trim().isEmpty()) {
            // discount flow is scaffolded here; actual service wiring follows later tasks
            JOptionPane.showMessageDialog(this, "Discount applied placeholder.");
        }
    }

    private void voidTransaction() {
        transactionService.voidTransaction(1L);
        refreshCart();
    }

    private void openPaymentDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Payment", true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.setSize(420, 220);
        dialog.setLocationRelativeTo(this);

        JLabel total = new JLabel("Total Due: " + totalLabel.getText());
        JLabel info = new JLabel("Payment dialog scaffold for task 18.");
        JButton close = new JButton("Close");

        close.addActionListener(e -> dialog.dispose());

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(total, BorderLayout.NORTH);
        content.add(info, BorderLayout.CENTER);
        content.add(close, BorderLayout.SOUTH);
        dialog.add(content);
        dialog.setVisible(true);
    }

    private int parseQty() {
        try {
            return Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Quantity must be a whole number.");
        }
    }

    private void clearInputs() {
        skuField.setText("");
        qtyField.setText("1");
    }

    private void refreshCart() {
        tableModel.setRowCount(0);
        List<CartLineItem> items = transactionService.getActiveCart(1L).getLineItems();
        for (CartLineItem item : items) {
            tableModel.addRow(new Object[] {
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineDiscounts().size(),
                item.getLineTotal()
            });
        }

        TransactionSummary summary = transactionService.calculateTotals(1L);
        subtotalLabel.setText(summary.subtotal().toPlainString());
        taxLabel.setText(summary.totalTax().toPlainString());
        discountLabel.setText(summary.totalDiscount().toPlainString());
        totalLabel.setText(summary.grandTotal().toPlainString());
    }
}
