package com.pos.ui;

import com.pos.domain.Role;
import com.pos.dto.AuthResult;
import com.pos.exception.AccountLockedException;
import com.pos.exception.ValidationException;
import com.pos.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final AuthService authService;
    private final Consumer<Role> roleRouter;

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Login");
    private final JLabel statusLabel = new JLabel(" ");

    public LoginPanel(AuthService authService, Consumer<Role> roleRouter) {
        this.authService = authService;
        this.roleRouter = roleRouter;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("POS System Login", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        form.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        form.add(passwordField, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        form.add(loginButton, gbc);

        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
    }

    private void attemptLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password are required.", Color.RED);
            return;
        }

        try {
            AuthResult result = authService.login(username, password);
            Role role = result.user().role();
            clearFields();
            if (roleRouter != null) {
                roleRouter.accept(role);
            }
            showStatus("Welcome, " + result.user().username() + "!", new Color(0, 120, 0));
        } catch (AccountLockedException ex) {
            showStatus("Account is locked. Please contact an administrator.", Color.RED);
        } catch (ValidationException ex) {
            showStatus(ex.getMessage(), Color.RED);
        } catch (Exception ex) {
            showStatus("Login failed: " + ex.getMessage(), Color.RED);
        }
    }

    public void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}
