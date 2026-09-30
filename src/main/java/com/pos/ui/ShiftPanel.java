package com.pos.ui;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class ShiftPanel extends JPanel {
    private final JTextField openingCashField = new JTextField("0.00");
    private final JTextField actualCashField = new JTextField("0.00");
    private final JLabel varianceLabel = new JLabel("0.00");
    private final JLabel statusLabel = new JLabel("Ready");

    public ShiftPanel() {
        buildUi();
        bindActions();
    }

    private void buildUi() {
        setLayout(new GridLayout(5, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(new JLabel("Opening cash:"));
        add(openingCashField);
        add(new JLabel("Actual cash:"));
        add(actualCashField);
        add(new JLabel("Variance:"));
        add(varianceLabel);
        add(new JLabel("Status:"));
        add(statusLabel);

        JButton openShiftButton = new JButton("Open Shift");
        JButton closeShiftButton = new JButton("Close Shift");
        add(openShiftButton);
        add(closeShiftButton);
    }

    private void bindActions() {
        Component[] components = getComponents();
        JButton openShiftButton = (JButton) components[components.length - 2];
        JButton closeShiftButton = (JButton) components[components.length - 1];

        openShiftButton.addActionListener(e -> {
            try {
                BigDecimal openingCash = new BigDecimal(openingCashField.getText().trim());
                statusLabel.setText("Shift opened with " + openingCash.toPlainString());
                varianceLabel.setText("0.00");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Opening cash must be a valid amount.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
            }
        });

        closeShiftButton.addActionListener(e -> {
            try {
                BigDecimal openingCash = new BigDecimal(openingCashField.getText().trim());
                BigDecimal actualCash = new BigDecimal(actualCashField.getText().trim());
                BigDecimal variance = actualCash.subtract(openingCash);
                varianceLabel.setText(variance.toPlainString());
                statusLabel.setText("Shift closed");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Actual cash must be a valid amount.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
