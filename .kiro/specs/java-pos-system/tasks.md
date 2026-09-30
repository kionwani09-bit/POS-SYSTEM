# Implementation Tasks: Java POS System

## Task 1: Project Setup
Set up the Maven project structure for the Java POS System.
- Create `pom.xml` at `c:\Users\HP\Desktop\POS SYSTEM\pom.xml` with:
  - Java 17, packaging jar
  - Dependencies: H2 2.2.224, BCrypt (jBCrypt 0.4), JUnit 5.10.0, Mockito 5.4.0, jqwik 1.8.0, AssertJ 3.24.2
  - Main class: `com.pos.Main`
- Create directory structure:
  - `src/main/java/com/pos/`
  - `src/main/resources/`
  - `src/test/java/com/pos/`
- Create `src/main/resources/schema.sql` with DDL for all tables: users, sessions, shifts, products, price_history, tax_categories, inventory, inventory_adjustments, transactions, line_items, payments, discounts, applied_discounts, refunds, refund_line_items, receipts, audit_log, system_config, gift_cards
- Create `src/main/resources/application.properties` with H2 connection settings
- Create `src/main/java/com/pos/Main.java` as the application entry point

## Task 2: Domain Model
Implement all domain model classes under `src/main/java/com/pos/domain/`.
- `Role.java` — enum: CASHIER, MANAGER, ADMINISTRATOR
- `User.java` — record with id, username, passwordHash, role, locked, failedAttempts, lastLogin
- `Product.java` — record with id, sku, name, description, unitPrice (BigDecimal), taxCategoryId, taxExempt, active
- `TaxCategory.java` — record with id, name, rate (BigDecimal)
- `Inventory.java` — record with id, productId, quantity, lowStockThreshold
- `CartLineItem.java` — class with productId, productName, sku, quantity, unitPrice, taxRate, taxExempt, lineDiscounts; methods: getPreTaxTotal(), getTaxAmount(), getLineTotal()
- `Cart.java` — class with sessionId, lineItems list, discounts list; methods: getSubtotal(), getTotalTax(), getTotalDiscount(), getGrandTotal() — grand total floored at zero
- `PaymentMethod.java` — enum: CASH, CREDIT_CARD, DEBIT_CARD, GIFT_CARD
- `PaymentEntry.java` — record with method, amount (BigDecimal), reference
- `TransactionStatus.java` — enum: PENDING, COMPLETED, VOIDED, REFUNDED, PARTIALLY_REFUNDED
- `CompletedTransaction.java` — record with transactionId, cashierId, shiftId, lineItems, payments, subtotal, totalTax, totalDiscount, grandTotal, completedAt, status
- `DiscountType.java` — enum: PERCENTAGE, FIXED_AMOUNT
- `DiscountScope.java` — enum: LINE_ITEM, TRANSACTION
- `Discount.java` — record with id, code, name, type, value, scope, startDate, endDate
- `AppliedDiscount.java` — record with discountId, name, type, scope, amountDeducted
- `Shift.java` — record with id, cashierId, managerId, startTime, endTime, openingCash, expectedClosingCash, actualClosingCash, cashVariance, status
- `AuditEventType.java` — enum: LOGIN, LOGOUT, ACCOUNT_LOCKED, ACCOUNT_UNLOCKED, TRANSACTION_COMPLETED, TRANSACTION_VOIDED, REFUND_COMPLETED, PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED, INVENTORY_ADJUSTED, DISCOUNT_APPLIED, SHIFT_OPENED, SHIFT_CLOSED
- `AuditEntry.java` — record with id, eventType, userId, eventTime, description, previousValue, newValue, checksum
- `GiftCard.java` — record with id, cardNumber, balance, issuedAt, expiresAt

## Task 3: Database and Connection Layer
Implement database connectivity under `src/main/java/com/pos/db/`.
- `DatabaseManager.java` — singleton managing H2 DataSource, runs schema.sql on first init
- `TransactionManager.java` — wraps JDBC transactions: begin(), commit(), rollback(), executeInTransaction(Callable)
- `SessionContext.java` — ThreadLocal holding current User and sessionId

## Task 4: Repository Layer
Implement JDBC repositories under `src/main/java/com/pos/repository/`.
- `UserRepository.java` — findByUsername, findById, save, update (locked, failedAttempts, lastLogin)
- `ProductRepository.java` — findBySku, findById, search(query), save, softDelete, findAll
- `InventoryRepository.java` — findByProductId, updateQuantity, findLowStock
- `InventoryAdjustmentRepository.java` — save adjustment record
- `TaxCategoryRepository.java` — findAll, findById, save, update
- `TransactionRepository.java` — save, findByTransactionId, findByShiftId, findByDateRange
- `LineItemRepository.java` — saveAll(list), findByTransactionId
- `PaymentRepository.java` — saveAll, findByTransactionId
- `DiscountRepository.java` — findByCode, findActive, save
- `AppliedDiscountRepository.java` — saveAll, findByTransactionId
- `RefundRepository.java` — save, findByTransactionId, findByRefundId
- `ReceiptRepository.java` — save, findByTransactionId
- `AuditLogRepository.java` — save, search(userId, eventType, from, to)
- `ShiftRepository.java` — save, findById, update, findActiveByUserId
- `GiftCardRepository.java` — findByCardNumber, updateBalance
- `SystemConfigRepository.java` — findByKey, save, findAll
- `PriceHistoryRepository.java` — save

## Task 5: Service Interfaces
Create service interfaces under `src/main/java/com/pos/service/`.
- `AuthService.java`
- `ProductService.java`
- `InventoryService.java`
- `TransactionService.java`
- `DiscountService.java`
- `TaxService.java`
- `PaymentService.java`
- `ReceiptService.java`
- `RefundService.java`
- `ReportService.java`
- `ShiftService.java`
- `AuditService.java`
- `ConfigService.java`
Match the exact method signatures defined in design.md.

## Task 6: Auth and Config Services
Implement under `src/main/java/com/pos/service/impl/`.
- `AuthServiceImpl.java`:
  - Hash passwords with BCrypt
  - Increment failedAttempts on bad login; lock account at 3 failures atomically
  - Start inactivity timer (5 min) on successful login
  - Log LOGIN, LOGOUT, ACCOUNT_LOCKED, ACCOUNT_UNLOCKED audit events
- `ConfigServiceImpl.java`:
  - Load from system_config table into a ConcurrentHashMap cache
  - Update cache immediately on save (no restart needed except gateway creds)
  - Validate required keys at startup; throw if missing

## Task 7: Product and Inventory Services
Implement under `src/main/java/com/pos/service/impl/`.
- `ProductServiceImpl.java`:
  - Validate unique SKU and non-negative price on create
  - Soft-delete (set active=false)
  - Write price_history on price change
  - CSV import: parse each row, skip invalid rows, accumulate ImportResult
  - Log PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED audit events
- `InventoryServiceImpl.java`:
  - adjustInventory: update quantity delta, write adjustment record, log INVENTORY_ADJUSTED
  - getLowStockProducts: query inventory where quantity <= lowStockThreshold
  - generateInventoryReport: return all products with qty, threshold, reorder flag

## Task 8: Tax and Discount Services
Implement under `src/main/java/com/pos/service/impl/`.
- `TaxServiceImpl.java`:
  - calculateLineTax: if taxExempt return ZERO, else unitPrice * qty * rate (BigDecimal, HALF_UP)
  - calculateTransactionTax: sum of all line taxes
- `DiscountServiceImpl.java`:
  - applyDiscountCode: look up code, check active+date range, compute amount, add to cart; throw if invalid/expired; log DISCOUNT_APPLIED
  - Auto-apply active promotions to cart line items on addItem
  - Enforce: total discounts cannot reduce grand total below zero

## Task 9: Transaction Service
Implement `TransactionServiceImpl.java` under `src/main/java/com/pos/service/impl/`.
- Maintain Map<Long, Cart> of active carts keyed by sessionId
- addItem: check inventory > 0 (throw OutOfStockException if not), look up product and tax category, create CartLineItem, auto-apply active promotions
- removeItem / updateQuantity: mutate cart; remove line if qty == 0
- calculateTotals: delegate to Cart methods
- completeTransaction: validate cart not empty; wrap in TransactionManager.executeInTransaction:
  - Insert transaction record
  - Insert all line items
  - Insert all payments
  - Decrement inventory for each line item
  - Insert applied discounts
  - Generate receipt
  - Log TRANSACTION_COMPLETED
- voidTransaction: clear cart, write void record, log TRANSACTION_VOIDED

## Task 10: Payment Service
Implement `PaymentServiceImpl.java` under `src/main/java/com/pos/service/impl/`.
- processPayment: route by PaymentMethod:
  - CASH: validate amount >= remaining total; compute change
  - CREDIT_CARD / DEBIT_CARD: call PaymentGateway.authorize(); throw on decline
  - GIFT_CARD: look up card, deduct balance (partial if insufficient), return remainder
- calculateChange: tendered - grandTotal, floor at zero

## Task 11: Receipt Service
Implement `ReceiptServiceImpl.java` under `src/main/java/com/pos/service/impl/`.
- generateReceipt: build Receipt content string from CompletedTransaction + config (store name/address, header/footer)
- printReceipt: call ReceiptPrinter.print(); on PrinterUnavailableException save to DB and throw for UI to show retry
- emailReceipt: use JavaMail (or log stub) to send receipt content
- getReceiptByTransactionId: fetch from receipt table

## Task 12: Refund Service
Implement `RefundServiceImpl.java` under `src/main/java/com/pos/service/impl/`.
- lookupTransaction: fetch by transactionId, return full detail with line items
- processRefund: validate managerId has MANAGER role; wrap in TransactionManager:
  - Insert refund record and refund line items
  - Increment inventory for each refunded line item
  - Attempt payment reversal (or log alternative method approval)
  - Generate refund receipt
  - Log REFUND_COMPLETED

## Task 13: Report and Shift Services
Implement under `src/main/java/com/pos/service/impl/`.
- `ReportServiceImpl.java`:
  - getDailySalesReport: aggregate transactions for date
  - getProductSalesReport: aggregate by product for date range
  - getCashierReport: aggregate by cashier
  - getShiftSummaryReport: totals for shift
  - exportReportAsCsv: write to temp file, return Path
- `ShiftServiceImpl.java`:
  - openShift: record opening cash, start time, log SHIFT_OPENED
  - closeShift: compute expectedClosing = openingCash + cashSales - cashRefunds; record variance; log SHIFT_CLOSED; lock cashier until new shift

## Task 14: Audit Service
Implement `AuditServiceImpl.java` under `src/main/java/com/pos/service/impl/`.
- log: compute SHA-256 checksum of (eventType+userId+timestamp+description), insert into audit_log
- search: query by userId, eventType, date range with parameterized SQL

## Task 15: Hardware Abstractions
Create under `src/main/java/com/pos/hardware/`.
- `BarcodeScanner.java` — interface
- `ReceiptPrinter.java` — interface with print(Receipt), isAvailable()
- `PaymentGateway.java` — interface with authorize(CardPaymentRequest)
- `MockBarcodeScanner.java` — simulates scan events via a scheduled executor
- `MockReceiptPrinter.java` — logs receipt content, configurable to simulate unavailability
- `MockPaymentGateway.java` — returns APPROVED for amounts < 10000, DECLINED otherwise

## Task 16: Swing UI — Login Screen
Create `src/main/java/com/pos/ui/LoginPanel.java`.
- Username and password fields, Login button
- On submit: call AuthService.login(), route to appropriate panel based on Role
- Display lockout message on AccountLockedException
- Inactivity timer triggers return to login screen

## Task 17: Swing UI — POS / Cart Screen
Create `src/main/java/com/pos/ui/CartPanel.java`.
- SKU input field + Add button (also listens to BarcodeScanner events)
- JTable showing line items: product name, qty, unit price, discounts, line total
- Footer row: subtotal, tax, discounts, grand total
- Buttons: Remove Item, Update Qty, Apply Discount Code, Void Transaction, Pay
- Pay button opens PaymentDialog

## Task 18: Swing UI — Payment Dialog
Create `src/main/java/com/pos/ui/PaymentDialog.java`.
- Shows grand total due
- Tabs or sections for each PaymentMethod
- Cash: tender amount field, shows change due
- Card: card number / reference field, calls gateway
- Gift Card: card number field, shows balance applied + remainder
- Supports split payment: add payment entries until total is covered
- On complete: calls TransactionService.completeTransaction(), shows receipt

## Task 19: Swing UI — Product Management Screen
Create `src/main/java/com/pos/ui/ProductPanel.java`.
- Search bar (by SKU or name)
- JTable of results
- Add / Edit / Delete buttons (Admin only)
- ProductFormDialog for create/edit
- Import CSV button

## Task 20: Swing UI — Inventory Screen
Create `src/main/java/com/pos/ui/InventoryPanel.java`.
- JTable of all products with current qty and threshold
- Low-stock items highlighted in yellow
- Manual Adjust button: opens dialog for delta + reason
- Generate Report button

## Task 21: Swing UI — Reports Screen
Create `src/main/java/com/pos/ui/ReportsPanel.java`.
- Tabs: Daily Sales, Product Sales, Cashier Performance, Shift Summary
- Date range pickers
- Generate button populates JTable with results
- Export CSV button

## Task 22: Swing UI — Shift and Config Screens
Create `src/main/java/com/pos/ui/ShiftPanel.java` and `src/main/java/com/pos/ui/ConfigPanel.java`.
- ShiftPanel: Open Shift (enter opening cash), Close Shift (enter actual cash, shows variance)
- ConfigPanel: Form fields for all system_config keys, Save button (Admin only)

## Task 23: Main Application Frame
Create `src/main/java/com/pos/ui/MainFrame.java` and update `Main.java`.
- JFrame with CardLayout switching between panels based on logged-in role
- Initialize DatabaseManager, load config, wire all services
- Check for missing critical config on startup; show blocking error if missing

## Task 24: Property-Based Tests
Create test class `src/test/java/com/pos/pbt/PosSystemPropertiesTest.java` using jqwik.
Implement all 10 properties from design.md:
1. Grand total is never negative (Cart with arbitrary line items + discounts)
2. Transaction tax equals sum of line taxes (TaxService)
3. Inventory decrements atomically by purchased quantity (TransactionService integration)
4. Refund increments inventory by refunded quantity (RefundService integration)
5. Cash change is always non-negative and correct (PaymentService)
6. Invalid/expired discount codes do not modify cart (DiscountService)
7. No plaintext password stored (AuthService + UserRepository)
8. Every critical action produces exactly one audit log entry (AuditService)
9. Split payment amounts sum to grand total (TransactionService)
10. Tax-exempt products have zero tax (TaxService)
Each property tagged: `@Tag("Feature: java-pos-system, Property N: <text>")`

## Task 25: Unit Tests
Create unit test classes under `src/test/java/com/pos/`:
- `AuthServiceTest.java` — login success/failure, lockout at 3 failures, BCrypt hash verification
- `CartTest.java` — totals calculation, discount clamping, line item removal at qty zero
- `TaxServiceTest.java` — rate application, tax-exempt zero, linearity
- `DiscountServiceTest.java` — percentage vs fixed, expired code rejection, grand total floor
- `PaymentServiceTest.java` — change calculation, insufficient cash, gift card partial, gateway decline
- `TransactionServiceTest.java` — empty cart rejection, out-of-stock rejection, void clears cart
- `InventoryServiceTest.java` — low-stock detection, adjustment recording
- `ProductServiceTest.java` — duplicate SKU rejection, soft delete, CSV partial import
