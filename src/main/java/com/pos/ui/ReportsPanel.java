package com.pos.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportsPanel extends JPanel {
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField fromField = new JTextField(LocalDate.now().minusDays(7).toString(), 12);
    private final JTextField toField = new JTextField(LocalDate.now().toString(), 12);

    private final DefaultTableModel dailyModel = new DefaultTableModel(
        new Object[]{"Period", "Transactions", "Revenue", "Tax", "Net Sales"}, 0);
    private final DefaultTableModel productModel = new DefaultTableModel(
        new Object[]{"SKU", "Product", "Units Sold", "Revenue"}, 0);
    private final DefaultTableModel cashierModel = new DefaultTableModel(
        new Object[]{"Cashier", "Transactions", "Revenue", "Refunds"}, 0);
    private final DefaultTableModel shiftModel = new DefaultTableModel(
        new Object[]{"Shift", "Open Cash", "Actual Cash", "Variance", "Status"}, 0);

    public ReportsPanel() {
        buildUi();
        generateSampleData();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("From:"));
        toolbar.add(fromField);
        toolbar.add(new JLabel("To:"));
        toolbar.add(toField);

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> generateSampleData());
        toolbar.add(generateButton);

        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "CSV export is ready for the report service integration step."));
        toolbar.add(exportButton);

        tabs.addTab("Daily Sales", new JScrollPane(new JTable(dailyModel)));
        tabs.addTab("Product Sales", new JScrollPane(new JTable(productModel)));
        tabs.addTab("Cashier Performance", new JScrollPane(new JTable(cashierModel)));
        tabs.addTab("Shift Summary", new JScrollPane(new JTable(shiftModel)));

        add(toolbar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private void generateSampleData() {
        dailyModel.setRowCount(0);
        dailyModel.addRow(new Object[]{LocalDate.now().toString(), 42, new BigDecimal("2840.75"), new BigDecimal("188.25"), new BigDecimal("2652.50")});

        productModel.setRowCount(0);
        productModel.addRow(new Object[]{"SKU-1001", "Coffee Beans", 28, new BigDecimal("420.00")} );
        productModel.addRow(new Object[]{"SKU-2004", "Sandwich Kit", 18, new BigDecimal("360.00")} );

        cashierModel.setRowCount(0);
        cashierModel.addRow(new Object[]{"alice", 17, new BigDecimal("1325.50"), new BigDecimal("42.60")} );
        cashierModel.addRow(new Object[]{"sam", 11, new BigDecimal("980.40"), new BigDecimal("18.20")} );

        shiftModel.setRowCount(0);
        shiftModel.addRow(new Object[]{"#101", new BigDecimal("250.00"), new BigDecimal("288.50"), new BigDecimal("38.50"), "Open"});
        shiftModel.addRow(new Object[]{"#102", new BigDecimal("310.00"), new BigDecimal("294.75"), new BigDecimal("-15.25"), "Closed"});
    }
}
