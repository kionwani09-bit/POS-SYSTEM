package com.pos.ui;

import com.pos.domain.PaymentEntry;
import com.pos.domain.PaymentMethod;
import com.pos.service.PaymentService;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class PaymentDialog extends JDialog {
    private final PaymentService paymentService;
    private final BigDecimal grandTotal;

    public PaymentDialog(Window owner, PaymentService paymentService, BigDecimal grandTotal) {
        super(owner, "Payment", ModalityType.APPLICATION_MODAL);
        this.paymentService = paymentService;
        this.grandTotal = grandTotal;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setSize(500, 260);
        setLocationRelativeTo(getOwner());

        JLabel totalLabel = new JLabel("Total Due: " + grandTotal.toPlainString());
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 18f));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Cash", buildCashTab());
        tabs.addTab("Card", buildCardTab());
        tabs.addTab("Gift Card", buildGiftCardTab());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(totalLabel, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        add(panel);
    }

    private JPanel buildCashTab() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField tenderField = new JTextField();
        JLabel changeLabel = new JLabel("Change: 0.00");

        panel.add(new JLabel("Tender amount:"));
        panel.add(tenderField);
        panel.add(new JLabel("Change due:"));
        panel.add(changeLabel);

        JButton apply = new JButton("Apply Cash");
        apply.addActionListener(e -> {
            try {
                BigDecimal tendered = new BigDecimal(tenderField.getText().trim());
                BigDecimal change = paymentService.calculateChange(tendered, grandTotal);
                changeLabel.setText("Change: " + change.toPlainString());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cash Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(apply);
        panel.add(new JLabel());
        return panel;
    }

    private JPanel buildCardTab() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextField refField = new JTextField();
        panel.add(new JLabel("Card reference:"));
        panel.add(refField);
        panel.add(new JLabel("Method:"));
        panel.add(new JLabel("CREDIT_CARD"));

        JButton apply = new JButton("Authorize Card");
        apply.addActionListener(e -> {
            try {
                paymentService.processPayment(
                    new PaymentEntry(PaymentMethod.CREDIT_CARD, grandTotal, refField.getText()),
                    grandTotal
                );
                JOptionPane.showMessageDialog(this, "Card payment authorized.");
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Payment Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(apply);
        panel.add(new JLabel());
        return panel;
    }

    private JPanel buildGiftCardTab() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextField cardField = new JTextField();
        panel.add(new JLabel("Gift card number:"));
        panel.add(cardField);
        panel.add(new JLabel("Applies to:"));
        panel.add(new JLabel(grandTotal.toPlainString()));

        JButton apply = new JButton("Apply Gift Card");
        apply.addActionListener(e -> {
            try {
                paymentService.processPayment(
                    new PaymentEntry(PaymentMethod.GIFT_CARD, grandTotal, cardField.getText()),
                    grandTotal
                );
                JOptionPane.showMessageDialog(this, "Gift card applied.");
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Gift Card Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(apply);
        panel.add(new JLabel());
        return panel;
    }
}
