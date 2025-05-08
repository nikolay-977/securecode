package ru.skillfactory.securecode.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.model.OtpConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OtpConfigDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigDao.class);
    private final Connection connection;

    public OtpConfigDao(Connection connection) {
        this.connection = connection;
    }

    public void update(int codeLength, int ttlSeconds) throws SQLException {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, codeLength);
            stmt.setInt(2, ttlSeconds);
            int updatedRows = stmt.executeUpdate();
            if (updatedRows == 0) {
                String insertSql = "INSERT INTO otp_config (code_length, ttl_seconds) VALUES (?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, codeLength);
                    insertStmt.setInt(2, ttlSeconds);
                    insertStmt.executeUpdate();
                    logger.info("Inserted new OTP config: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
                }
            } else {
                logger.info("Updated OTP config: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
            }
        } catch (SQLException e) {
            logger.error("Error updating OTP config: {}", e.getMessage(), e);
            throw e;
        }
    }

    public OtpConfig get() throws SQLException {
        String sql = "SELECT code_length, ttl_seconds FROM otp_config LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                OtpConfig config = new OtpConfig();
                config.codeLength = rs.getInt("code_length");
                config.ttlSeconds = rs.getInt("ttl_seconds");
                logger.info("Retrieved OTP config: codeLength={}, ttlSeconds={}", config.codeLength, config.ttlSeconds);
                return config;
            } else {
                logger.warn("No OTP config found in the database.");
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error retrieving OTP config: {}", e.getMessage(), e);
            throw e;
        }
    }
}
