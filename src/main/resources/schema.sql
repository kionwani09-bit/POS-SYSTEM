CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    shift_id BIGINT,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shifts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cashier_id BIGINT NOT NULL REFERENCES users(id),
    manager_id BIGINT NOT NULL REFERENCES users(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    opening_cash DECIMAL(12,2) NOT NULL,
    expected_closing_cash DECIMAL(12,2),
    actual_closing_cash DECIMAL(12,2),
    cash_variance DECIMAL(12,2),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE IF NOT EXISTS tax_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(6,4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    unit_price DECIMAL(12,2) NOT NULL,
    tax_category_id BIGINT REFERENCES tax_categories(id),
    tax_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    previous_price DECIMAL(12,2) NOT NULL,
    new_price DECIMAL(12,2) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by_user_id BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 10,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    previous_qty INT NOT NULL,
    new_qty INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    adjusted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    session_id BIGINT NOT NULL REFERENCES sessions(id),
    cashier_id BIGINT NOT NULL REFERENCES users(id),
    shift_id BIGINT NOT NULL REFERENCES shifts(id),
    subtotal DECIMAL(12,2) NOT NULL,
    total_tax DECIMAL(12,2) NOT NULL,
    total_discount DECIMAL(12,2) NOT NULL,
    grand_total DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    tax_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reference VARCHAR(255),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    value DECIMAL(12,2) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS applied_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    discount_id BIGINT NOT NULL REFERENCES discounts(id),
    amount_deducted DECIMAL(12,2) NOT NULL,
    scope VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_id VARCHAR(50) NOT NULL UNIQUE,
    original_transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    manager_id BIGINT NOT NULL REFERENCES users(id),
    refund_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refund_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_id BIGINT NOT NULL REFERENCES refunds(id),
    original_line_item_id BIGINT NOT NULL REFERENCES line_items(id),
    refunded_qty INT NOT NULL,
    refunded_amount DECIMAL(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE REFERENCES transactions(id),
    content CLOB NOT NULL,
    printed BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(2000),
    previous_value VARCHAR(1000),
    new_value VARCHAR(1000),
    checksum VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(1000) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_user_id BIGINT REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS gift_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_number VARCHAR(100) NOT NULL UNIQUE,
    balance DECIMAL(12,2) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Default seed data
INSERT INTO system_config (config_key, config_value) VALUES
    ('store.name', 'My Store'),
    ('store.address', '123 Main St'),
    ('receipt.header', 'Thank you for shopping!'),
    ('receipt.footer', 'Please come again.'),
    ('currency', 'USD'),
    ('lowstock.threshold', '10'),
    ('session.timeout.minutes', '5'),
    ('gateway.url', 'http://localhost:8080/gateway'),
    ('printer.port', 'COM1')
ON CONFLICT (config_key) DO NOTHING;
