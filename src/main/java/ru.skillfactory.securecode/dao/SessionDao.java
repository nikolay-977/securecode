package ru.skillfactory.securecode.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SessionDao {
    private final Connection connection;

    public SessionDao(Connection connection) {
        this.connection = connection;
    }

    public void saveSession(String token, UUID userId) throws SQLException {
        String sql = "INSERT INTO sessions (id, token, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, UUID.randomUUID());
            stmt.setString(2, token);
            stmt.setObject(3, userId);
            stmt.executeUpdate();
        }
    }

    public UUID findUserIdByToken(String token) throws SQLException {
        String sql = "SELECT user_id FROM sessions WHERE token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("user_id");
            }
        }
        return null;
    }

    public void deleteSession(String token) throws SQLException {
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        }
    }
}
