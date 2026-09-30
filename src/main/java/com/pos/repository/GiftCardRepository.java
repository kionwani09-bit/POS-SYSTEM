package com.pos.repository;

import com.pos.db.DatabaseManager;
import com.pos.domain.GiftCard;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class GiftCardRepository {
    private final DatabaseManager db;

    public GiftCardRepository(DatabaseManager db) { this.db = db; }

    public Optional<GiftCard> findByCardNumber(String cardNumber) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM gift_cards WHERE card_number=?")) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp exp = rs.getTimestamp("expires_at");
                    return Optional.of(new GiftCard(
                        rs.getLong("id"), rs.getString("card_number"),
                        rs.getBigDecimal("balance"),
                        rs.getTimestamp("issued_at").toInstant(),
                        exp != null ? exp.toInstant() : null));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void updateBalance(String cardNumber, BigDecimal newBalance) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE gift_cards SET balance=? WHERE card_number=?")) {
            ps.setBigDecimal(1, newBalance); ps.setString(2, cardNumber);
            ps.executeUpdate(); c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void updateBalanceInTx(Connection conn, String cardNumber, BigDecimal newBalance) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE gift_cards SET balance=? WHERE card_number=?")) {
            ps.setBigDecimal(1, newBalance); ps.setString(2, cardNumber); ps.executeUpdate();
        }
    }
}
