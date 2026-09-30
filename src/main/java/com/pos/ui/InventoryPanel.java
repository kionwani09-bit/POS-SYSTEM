package com.pos.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class InventoryPanel extends JPanel {
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Product", "Qty", "Threshold", "Low Stock"}, 0);
    private final JTable table = new JTable(tableModel);

    public InventoryPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JButton("Adjust"));
        actions.add(new JButton("Generate Report"));

        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        tableModel.addRow(new Object[]{"Sample Item", 8, 10, true});
        tableModel.addRow(new Object[]{"Sample Item 2", 25, 10, false});
    }
}
