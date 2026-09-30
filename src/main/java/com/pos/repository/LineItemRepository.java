package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.CartLineItem;

import java.sql.*;
import java.util.*;

public class LineItemRepository {
    private final DatabaseManager db;

    public LineItemRepository(DatabaseManager db) { this.db = db; }

    /** Inserts all line items within an existing connection/transaction. */
    public void saveAllInTx(Connection conn, long transactionDbId, List<CartLineItem> items) throws SQLException {
        String sql = "INSERT INTO line_items "
            + "(transaction_id,product_id,quantity,unit_price,tax_amount,discount_amount,line_total)"
            + " VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CartLineItem item : items) {
                ps.setLong(1, transactionDbId); ps.setLong(2, item.getProductId());
                ps.setInt(3, item.getQuantity()); ps.setBigDecimal(4, item.getUnitPrice());
                ps.setBigDecimal(5, item.getTaxAmount()); ps.setBigDecimal(6, item.getLineDiscountTotal());
                ps.setBigDecimal(7, item.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Map<String, Object>> findByTransactionDbId(long transactionDbId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT li.*, p.sku, p.name FROM line_items li "
            + "JOIN products p ON li.product_id=p.id WHERE li.transaction_id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, transactionDbId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("productId", rs.getLong("product_id"));
                    row.put("sku", rs.getString("sku"));
                    row.put("name", rs.getString("name"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("unitPrice", rs.getBigDecimal("unit_price"));
                    row.put("taxAmount", rs.getBigDecimal("tax_amount"));
                    row.put("discountAmount", rs.getBigDecimal("discount_amount"));
                    row.put("lineTotal", rs.getBigDecimal("line_total"));
                    result.add(row);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }
}
