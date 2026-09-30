package com.pos.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ProductPanel extends JPanel {
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"SKU", "Name", "Price", "Active"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);

    public ProductPanel() {
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        top.add(new JButton("Search"));
        top.add(new JButton("Add"));
        top.add(new JButton("Edit"));
        top.add(new JButton("Delete"));
        top.add(new JButton("Import CSV"));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        tableModel.addRow(new Object[]{"SKU-001", "Sample Item", "25.00", true});
        tableModel.addRow(new Object[]{"SKU-002", "Sample Item 2", "40.50", true});
    }
}
