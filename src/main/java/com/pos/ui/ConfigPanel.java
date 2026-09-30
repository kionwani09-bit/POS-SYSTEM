package com.pos.ui;

import javax.swing.*;
import java.awt.*;

public class ConfigPanel extends JPanel {
    private final JTextField storeNameField = new JTextField("My Store");
    private final JTextField storeAddressField = new JTextField("123 Main St");
    private final JTextField sessionTimeoutField = new JTextField("5");
    private final JTextField lowStockThresholdField = new JTextField("10");
    private final JTextField gatewayUrlField = new JTextField("http://localhost:8080/gateway");
    private final JTextField printerPortField = new JTextField("COM1");

    public ConfigPanel() {
        buildUi();
        bindActions();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("Store name:"));
        form.add(storeNameField);
        form.add(new JLabel("Store address:"));
        form.add(storeAddressField);
        form.add(new JLabel("Session timeout:"));
        form.add(sessionTimeoutField);
        form.add(new JLabel("Low stock threshold:"));
        form.add(lowStockThresholdField);
        form.add(new JLabel("Gateway URL:"));
        form.add(gatewayUrlField);
        form.add(new JLabel("Printer port:"));
        form.add(printerPortField);

        JButton saveButton = new JButton("Save Settings");
        add(form, BorderLayout.CENTER);
        add(saveButton, BorderLayout.SOUTH);
    }

    private void bindActions() {
        Component[] components = getComponents();
        JButton saveButton = (JButton) components[components.length - 1];
        saveButton.addActionListener(e -> {
            String summary = String.format(
                "Store: %s%nAddress: %s%nTimeout: %s%nLow stock: %s%nGateway: %s%nPrinter: %s",
                storeNameField.getText(),
                storeAddressField.getText(),
                sessionTimeoutField.getText(),
                lowStockThresholdField.getText(),
                gatewayUrlField.getText(),
                printerPortField.getText()
            );
            JOptionPane.showMessageDialog(this, "Settings saved successfully.\n\n" + summary,
                "Configuration", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
