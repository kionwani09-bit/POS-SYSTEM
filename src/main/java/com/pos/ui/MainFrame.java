package com.pos.ui;

import com.pos.db.DatabaseManager;
import com.pos.domain.Role;
import com.pos.service.AuthService;
import com.pos.service.ConfigService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public MainFrame(DatabaseManager databaseManager,
                    AuthService authService,
                    ConfigService configService) {
        super("POS System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900);
        setLocationRelativeTo(null);

        LoginPanel loginPanel = new LoginPanel(authService, role -> {
            switch (role) {
                case CASHIER -> cardLayout.show(cards, "CASHIER");
                case MANAGER -> cardLayout.show(cards, "MANAGER");
                case ADMINISTRATOR -> cardLayout.show(cards, "ADMIN");
                default -> cardLayout.show(cards, "LOGIN");
            }
        });

        JPanel cashierPanel = buildCashierPanel();
        JPanel managerPanel = buildManagerPanel();
        JPanel adminPanel = buildAdminPanel();

        cards.add(loginPanel, "LOGIN");
        cards.add(cashierPanel, "CASHIER");
        cards.add(managerPanel, "MANAGER");
        cards.add(adminPanel, "ADMIN");
        add(cards);

        cardLayout.show(cards, "LOGIN");
    }

    private JPanel buildCashierPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(new JLabel("Cashier Console", SwingConstants.CENTER), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(new CartPanel(null, null));
        split.setRightComponent(new ProductPanel());
        split.setResizeWeight(0.75);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("Manager Console", SwingConstants.CENTER), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventory", new InventoryPanel());
        tabs.addTab("Reports", new ReportsPanel());
        tabs.addTab("Shift", new ShiftPanel());
        tabs.addTab("Config", new ConfigPanel());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("Administrator Console", SwingConstants.CENTER), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Products", new ProductPanel());
        tabs.addTab("Inventory", new InventoryPanel());
        tabs.addTab("Reports", new ReportsPanel());
        tabs.addTab("Config", new ConfigPanel());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }
}
