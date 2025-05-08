package ru.skillfactory.securecode.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.model.OtpCode;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;


public class OtpDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpDao.class);
    private final Connection connection;

    public OtpDao(Connection connection) {
        this.connection = connection;
    }

    public void save(OtpCode otp) {
        String sql = "INSERT INTO otp_codes (user_id, code, status, operation_id, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, otp.userId);
            stmt.setString(2, otp.code);
            stmt.setString(3, otp.status);
            stmt.setString(4, otp.operationId);
            stmt.setTimestamp(5, Timestamp.valueOf(otp.createdAt));
            stmt.setTimestamp(6, Timestamp.valueOf(otp.expiresAt));
            stmt.executeUpdate();
            logger.debug("Saved OTP code for userId: {}, operationId: {}", otp.userId, otp.operationId);
        } catch (SQLException e) {
            logger.error("Error saving OTP code for userId: {}: {}", otp.userId, e.getMessage(), e);
        }
    }

    public Optional<OtpCode> findActiveByUserAndOperation(UUID userId, String operationId) {
        String sql = "SELECT * FROM otp_codes WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, userId, java.sql.Types.OTHER);
            stmt.setString(2, operationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                OtpCode otp = new OtpCode();
                otp.id = UUID.fromString(rs.getString("id"));
                otp.userId = UUID.fromString(rs.getString("user_id"));
                otp.code = rs.getString("code");
                otp.status = rs.getString("status");
                otp.operationId = rs.getString("operation_id");
                otp.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                otp.expiresAt = rs.getTimestamp("expires_at").toLocalDateTime();
                logger.debug("Found active OTP code for userId: {}, operationId: {}", userId, operationId);
                return Optional.of(otp);
            } else {
                logger.warn("No active OTP code found for userId: {}, operationId: {}", userId, operationId);
            }
        } catch (SQLException e) {
            logger.error("Error finding active OTP code for userId: {}: {}", userId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void updateStatus(UUID id, String status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setObject(2, id, java.sql.Types.OTHER);
            stmt.executeUpdate();
            logger.debug("Updated OTP code status to '{}' for id: {}", status, id);
        } catch (SQLException e) {
            logger.error("Error updating OTP code status for id: {}: {}", id, e.getMessage(), e);
        }
    }

    public void expireOldOtps() {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < now()";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int updatedRows = stmt.executeUpdate();
            logger.debug("Expired {} old OTP codes.", updatedRows);
        } catch (SQLException e) {
            logger.error("Error expiring old OTP codes: {}", e.getMessage(), e);
        }
    }

    public void deleteByUserId(UUID userId) throws SQLException {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, userId, java.sql.Types.OTHER);
            stmt.executeUpdate();
            logger.debug("Deleted OTP codes for userId: {}", userId);
        } catch (SQLException e) {
            logger.error("Error deleting OTP codes for userId {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}