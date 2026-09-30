# Requirements Document

## Introduction

This document defines the requirements for a Java-based Point of Sale (POS) System. The system enables retail businesses to process customer transactions, manage inventory, handle payments, generate receipts, and produce sales reports. The system is designed for use by cashiers, store managers, and administrators, and must operate reliably in a retail environment with support for barcode scanning, multiple payment methods, and end-of-day reconciliation.

---

## Glossary

- **POS_System**: The complete Java-based Point of Sale application
- **Cashier**: A store employee who operates the POS terminal to process customer transactions
- **Manager**: A store employee with elevated privileges who can perform voids, refunds, and view reports
- **Administrator**: A user with full system access who manages products, users, and system configuration
- **Transaction**: A single sales session consisting of one or more line items and a payment
- **Line_Item**: A single product entry within a transaction, including quantity and price
- **Cart**: The in-progress collection of line items for a current transaction
- **Product**: A sellable item in the system, identified by a barcode or SKU
- **SKU**: Stock Keeping Unit — a unique alphanumeric identifier for a product
- **Barcode**: A machine-readable optical representation of a product's SKU
- **Inventory**: The tracked quantity of each product available in the store
- **Payment**: The monetary settlement of a transaction using one or more payment methods
- **Payment_Method**: The mode of payment used (cash, credit card, debit card, or gift card)
- **Receipt**: A printed or digital record of a completed transaction
- **Refund**: A reversal of a completed transaction that returns funds to the customer
- **Void**: The cancellation of a transaction before payment is finalized
- **Discount**: A reduction applied to a line item or the total transaction amount
- **Tax**: A government-mandated percentage applied to taxable products in a transaction
- **Report**: A summary of transactions, sales, or inventory over a defined time period
- **Session**: A cashier's work period starting at login and ending at logout
- **Shift**: A defined time period during which a cashier operates the POS terminal
- **Database**: The persistent data store used by the POS_System to store all records
- **Barcode_Scanner**: A hardware input device that reads product barcodes and sends the SKU to the POS_System
- **Receipt_Printer**: A hardware output device that prints transaction receipts

---

## Requirements

### Requirement 1: User Authentication and Access Control

**User Story:** As a store employee, I want to log in to the POS system with my credentials, so that my actions are tracked and unauthorized access is prevented.

#### Acceptance Criteria

1. THE POS_System SHALL require a username and password before granting access to any functional screen.
2. WHEN a Cashier provides valid credentials, THE POS_System SHALL grant access to the cashier-level interface within 2 seconds.
3. WHEN a Manager provides valid credentials, THE POS_System SHALL grant access to both cashier-level and manager-level functions.
4. WHEN an Administrator provides valid credentials, THE POS_System SHALL grant access to all system functions including product management and user management.
5. IF a user provides an incorrect password three consecutive times, THEN THE POS_System SHALL lock that user account and display a lockout message.
6. WHEN a locked account attempts to log in, THE POS_System SHALL display a lockout message and require an Administrator to unlock the account.
7. WHEN a logged-in user is inactive for 5 consecutive minutes, THE POS_System SHALL lock the screen and require re-authentication.
8. THE POS_System SHALL store all passwords as hashed values using a secure one-way hashing algorithm and SHALL NOT store plaintext passwords.
9. WHEN a user logs out, THE POS_System SHALL end the user's Session and record the logout timestamp.

---

### Requirement 2: Product Management

**User Story:** As an Administrator, I want to manage the product catalog, so that the POS system always reflects current product information.

#### Acceptance Criteria

1. THE POS_System SHALL maintain a product catalog where each Product record contains: SKU, name, description, unit price, tax category, and current inventory quantity.
2. WHEN an Administrator creates a new Product, THE POS_System SHALL validate that the SKU is unique and that the unit price is a non-negative numeric value before saving.
3. WHEN an Administrator updates a Product's unit price, THE POS_System SHALL record the previous price, the new price, and the timestamp of the change.
4. WHEN an Administrator deletes a Product, THE POS_System SHALL mark the Product as inactive rather than removing it from the Database, preserving historical transaction records.
5. THE POS_System SHALL support product search by SKU, name, or partial name match.
6. WHEN a search query is submitted, THE POS_System SHALL return all matching Products within 1 second.
7. THE POS_System SHALL support importing product data from a CSV file in the format: SKU, name, description, unit price, tax category, quantity.
8. IF a CSV import row contains invalid data, THEN THE POS_System SHALL skip that row, record the row number and error reason in an import error log, and continue processing remaining rows.

---

### Requirement 3: Inventory Management

**User Story:** As a Manager, I want the system to track product inventory levels, so that I can identify low-stock items and prevent selling out-of-stock products.

#### Acceptance Criteria

1. THE POS_System SHALL decrement the Inventory quantity of each Product by the purchased quantity when a Transaction is completed successfully.
2. WHEN a Refund is processed, THE POS_System SHALL increment the Inventory quantity of each refunded Product by the refunded quantity.
3. WHILE a Product's inventory quantity is at or below the configured low-stock threshold, THE POS_System SHALL display a low-stock warning to the Manager on the inventory dashboard.
4. IF a Cashier attempts to add a Product with zero inventory to the Cart, THEN THE POS_System SHALL display an out-of-stock error message and SHALL NOT add the item to the Cart.
5. THE POS_System SHALL allow a Manager to manually adjust the inventory quantity of any Product and SHALL record the reason, previous quantity, new quantity, and timestamp for each manual adjustment.
6. THE POS_System SHALL generate an inventory report listing all Products with their current quantities, low-stock threshold, and reorder status on demand.

---

### Requirement 4: Transaction Processing

**User Story:** As a Cashier, I want to add products to a cart and process a transaction, so that I can complete customer purchases efficiently.

#### Acceptance Criteria

1. THE POS_System SHALL maintain exactly one active Cart per Cashier Session at any given time.
2. WHEN the Barcode_Scanner sends a SKU to the POS_System, THE POS_System SHALL look up the Product and add one unit as a Line_Item to the active Cart within 500 milliseconds.
3. WHEN a Cashier manually enters a SKU, THE POS_System SHALL look up the Product and add it to the active Cart within 1 second.
4. WHEN a Cashier sets the quantity of a Line_Item to zero, THE POS_System SHALL remove that Line_Item from the Cart.
5. THE POS_System SHALL display the Cart in real time showing each Line_Item's product name, quantity, unit price, applied discounts, line total, subtotal, total tax, and grand total.
6. WHEN a Cashier initiates payment, THE POS_System SHALL calculate the grand total as the sum of all Line_Item totals plus all applicable taxes minus all applied discounts.
7. IF a Cashier attempts to initiate payment on an empty Cart, THEN THE POS_System SHALL display an error message and SHALL NOT proceed to the payment screen.
8. WHEN a Transaction is completed, THE POS_System SHALL assign a unique Transaction ID, record the timestamp, Cashier ID, all Line_Items, payment details, and totals to the Database.
9. WHEN a Cashier voids a Transaction before payment, THE POS_System SHALL clear the Cart and record a void event with the Cashier ID and timestamp.

---

### Requirement 5: Discount and Promotion Management

**User Story:** As a Manager, I want to apply discounts and promotions to products and transactions, so that I can support sales events and customer incentives.

#### Acceptance Criteria

1. THE POS_System SHALL support percentage-based discounts and fixed-amount discounts applied at the Line_Item level or the Transaction level.
2. WHEN a Manager configures a promotion, THE POS_System SHALL require a promotion name, discount type (percentage or fixed amount), discount value, and a start date and end date.
3. WHILE a promotion's start date and end date define an active period, THE POS_System SHALL automatically apply the promotion to all qualifying Cart Line_Items during that period.
4. WHEN a Cashier manually applies a discount code, THE POS_System SHALL validate the code and apply the corresponding Discount to the Cart within 1 second.
5. IF a discount code is expired or invalid, THEN THE POS_System SHALL display an error message and SHALL NOT apply the Discount to the Cart.
6. THE POS_System SHALL display each applied Discount as a separate line on the Cart summary, showing the discount name, type, and amount deducted.
7. THE POS_System SHALL enforce a maximum discount limit per Transaction so that the total of all discounts SHALL NOT reduce the grand total below zero.

---

### Requirement 6: Tax Calculation

**User Story:** As an Administrator, I want the system to calculate taxes accurately based on product tax categories, so that the business remains compliant with tax regulations.

#### Acceptance Criteria

1. THE POS_System SHALL support configurable tax categories, where each tax category has a name and a tax rate expressed as a percentage.
2. WHEN an Administrator creates or updates a tax category rate, THE POS_System SHALL apply the new rate to all new Transactions and SHALL NOT retroactively alter completed Transaction records.
3. WHEN the grand total is calculated, THE POS_System SHALL apply each Line_Item's tax category rate to that Line_Item's pre-tax total and sum all tax amounts as the total Tax for the Transaction.
4. THE POS_System SHALL display the Tax amount for each Line_Item and the total Tax amount as separate line items on the Cart summary and Receipt.
5. WHERE a Product is marked as tax-exempt, THE POS_System SHALL apply a zero tax rate to that Line_Item regardless of the assigned tax category.

---

### Requirement 7: Payment Processing

**User Story:** As a Cashier, I want to accept multiple payment methods, so that customers can pay using their preferred option.

#### Acceptance Criteria

1. THE POS_System SHALL accept the following Payment_Methods: cash, credit card, debit card, and gift card.
2. THE POS_System SHALL support split payments where a single Transaction is settled using more than one Payment_Method.
3. WHEN a Cashier enters a cash Payment amount that is greater than or equal to the grand total, THE POS_System SHALL calculate and display the change due to the customer.
4. IF a Cashier enters a cash Payment amount that is less than the grand total, THEN THE POS_System SHALL display an insufficient payment error and SHALL NOT complete the Transaction.
5. WHEN a card payment is submitted, THE POS_System SHALL send the payment request to the configured payment gateway and wait for an authorization response before completing the Transaction.
6. IF the payment gateway returns a declined response, THEN THE POS_System SHALL display a decline message and SHALL NOT mark the Transaction as complete.
7. WHEN a gift card payment is applied, THE POS_System SHALL validate the gift card balance, deduct the payment amount from the gift card balance, and record the updated balance in the Database.
8. IF a gift card balance is insufficient to cover the full Transaction total, THEN THE POS_System SHALL apply the full gift card balance as a partial payment and prompt the Cashier to collect the remaining amount using another Payment_Method.
9. WHEN a Transaction is completed successfully, THE POS_System SHALL update Inventory quantities and trigger Receipt generation within 2 seconds.

---

### Requirement 8: Receipt Generation

**User Story:** As a Cashier, I want the system to generate a receipt after each transaction, so that the customer has a record of their purchase.

#### Acceptance Criteria

1. WHEN a Transaction is completed, THE POS_System SHALL generate a Receipt containing: store name, store address, Transaction ID, date and time, Cashier name, each Line_Item with product name, quantity, unit price, and line total, all applied Discounts, total Tax, grand total, Payment_Method(s) used, amount tendered, and change due.
2. WHEN a Receipt is generated, THE POS_System SHALL send the Receipt to the Receipt_Printer within 3 seconds of Transaction completion.
3. IF the Receipt_Printer is unavailable, THEN THE POS_System SHALL display an error message, save the Receipt to the Database, and allow the Cashier to retry printing without re-processing the Transaction.
4. THE POS_System SHALL allow a Cashier to reprint a Receipt for any completed Transaction within the current Shift by entering the Transaction ID.
5. WHERE the customer has provided an email address, THE POS_System SHALL send a digital Receipt to that email address in addition to printing a physical Receipt.

---

### Requirement 9: Refund and Return Processing

**User Story:** As a Manager, I want to process refunds for returned products, so that customers can receive their money back for valid returns.

#### Acceptance Criteria

1. THE POS_System SHALL require Manager-level credentials to initiate a Refund.
2. WHEN a Manager initiates a Refund, THE POS_System SHALL require the original Transaction ID before displaying the Transaction details.
3. WHEN a Transaction ID is entered, THE POS_System SHALL retrieve and display the original Transaction details including all Line_Items, payment method, and grand total within 1 second.
4. THE POS_System SHALL allow partial refunds where a Manager selects one or more specific Line_Items and quantities to refund rather than the entire Transaction.
5. WHEN a Refund is processed, THE POS_System SHALL return the refund amount to the same Payment_Method used in the original Transaction.
6. IF the original Payment_Method is unavailable for the refund, THEN THE POS_System SHALL require a Manager to approve an alternative refund Payment_Method before proceeding.
7. WHEN a Refund is completed, THE POS_System SHALL record the Refund with: Refund ID, original Transaction ID, Manager ID, refunded Line_Items, refund amount, refund Payment_Method, and timestamp.
8. WHEN a Refund is completed, THE POS_System SHALL generate a Refund Receipt and send it to the Receipt_Printer.

---

### Requirement 10: Sales Reporting

**User Story:** As a Manager, I want to generate sales reports, so that I can monitor business performance and make informed decisions.

#### Acceptance Criteria

1. THE POS_System SHALL provide a daily sales report showing: total number of Transactions, total revenue, total tax collected, total discounts applied, total refunds processed, and net sales for a specified date.
2. THE POS_System SHALL provide a product sales report showing: each Product's name, SKU, total units sold, total revenue, and total refunded units for a specified date range.
3. THE POS_System SHALL provide a cashier performance report showing: each Cashier's name, total Transactions processed, total revenue, and total refunds processed for a specified Shift or date range.
4. WHEN a report is requested, THE POS_System SHALL generate and display the report within 5 seconds for date ranges up to 31 days.
5. THE POS_System SHALL allow a Manager to export any report in CSV format.
6. WHEN a report is exported, THE POS_System SHALL write the CSV file and make it available for download within 10 seconds.
7. THE POS_System SHALL provide an end-of-day report summarizing the total cash, credit card, debit card, and gift card payments collected during the current Shift.

---

### Requirement 11: Shift Management

**User Story:** As a Manager, I want to open and close cashier shifts, so that daily cash management and accountability are maintained.

#### Acceptance Criteria

1. WHEN a Cashier begins a Shift, THE POS_System SHALL record the opening cash drawer amount entered by the Manager and the Shift start timestamp.
2. WHEN a Manager closes a Shift, THE POS_System SHALL calculate the expected closing cash amount as: opening cash amount plus total cash sales minus total cash refunds during the Shift.
3. WHEN a Manager closes a Shift, THE POS_System SHALL require the Manager to enter the actual counted cash amount and SHALL record the cash variance as the difference between the expected and actual amounts.
4. WHEN a Shift is closed, THE POS_System SHALL prevent the associated Cashier account from processing new Transactions until a new Shift is opened.
5. THE POS_System SHALL generate a Shift summary report upon Shift closure containing: Cashier name, Shift start and end times, total Transactions, total revenue by Payment_Method, total discounts, total refunds, opening cash amount, expected closing cash, actual closing cash, and cash variance.

---

### Requirement 12: Audit Logging

**User Story:** As an Administrator, I want all critical system actions to be logged, so that I can audit activity and investigate discrepancies.

#### Acceptance Criteria

1. THE POS_System SHALL record an audit log entry for each of the following events: user login, user logout, account lockout, Transaction completion, Transaction void, Refund completion, Product creation, Product update, Product deletion, inventory adjustment, Discount application, and Shift open or close.
2. WHEN an audit log entry is created, THE POS_System SHALL record: event type, user ID, timestamp, and a description of the change including previous and new values where applicable.
3. THE POS_System SHALL retain audit log entries for a minimum of 90 days.
4. THE POS_System SHALL allow an Administrator to search audit logs by user ID, event type, or date range.
5. WHEN an audit log search is submitted, THE POS_System SHALL return matching results within 3 seconds.
6. THE POS_System SHALL store audit log entries in a tamper-evident manner so that existing entries cannot be modified or deleted through the application interface.

---

### Requirement 13: System Configuration

**User Story:** As an Administrator, I want to configure system-wide settings, so that the POS system operates according to the store's specific requirements.

#### Acceptance Criteria

1. THE POS_System SHALL allow an Administrator to configure the following settings: store name, store address, receipt header and footer text, default currency, tax categories and rates, low-stock threshold, payment gateway credentials, receipt printer connection, and session inactivity timeout.
2. WHEN a configuration setting is updated, THE POS_System SHALL apply the new setting to all subsequent operations without requiring a system restart, except for payment gateway credential changes.
3. WHEN payment gateway credentials are updated, THE POS_System SHALL require a system restart to apply the new credentials.
4. THE POS_System SHALL validate all configuration inputs and display a descriptive error message for any invalid value before saving.
5. IF a critical configuration value is missing at startup, THEN THE POS_System SHALL display a configuration error message identifying the missing setting and SHALL NOT allow the system to reach the login screen until the configuration is complete.

---

### Requirement 14: Data Persistence and Integrity

**User Story:** As a store owner, I want all transaction and inventory data to be stored reliably, so that no data is lost in the event of a system failure.

#### Acceptance Criteria

1. THE POS_System SHALL persist all Transaction records, Inventory changes, Refund records, audit log entries, and configuration data to the Database.
2. WHEN a Transaction is being written to the Database, THE POS_System SHALL use a database transaction to ensure that all related records (Transaction header, Line_Items, Payment records, and Inventory updates) are committed atomically.
3. IF a database write failure occurs during Transaction completion, THEN THE POS_System SHALL roll back all related database changes, display an error message to the Cashier, and log the failure in the audit log.
4. THE POS_System SHALL support scheduled database backups at a configurable interval and SHALL log the success or failure of each backup operation.
5. THE POS_System SHALL enforce referential integrity between Transaction records, Line_Items, Products, and User records so that orphaned records cannot be created through the application interface.
