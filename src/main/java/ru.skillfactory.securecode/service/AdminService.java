package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpConfigDao;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final UserDao userDao;
    private final OtpDao otpDao;
    private final OtpConfigDao otpConfigDao;

    public AdminService(Connection connection) {
        this.userDao = new UserDao(connection);
        this.otpDao = new OtpDao(connection);
        this.otpConfigDao = new OtpConfigDao(connection);
    }

    public List<User> listUsers() throws SQLException {
        logger.debug("Fetching list of users excluding admins.");
        List<User> users = userDao.findAllUsersExcludingAdmins();
        logger.debug("Fetched {} users.", users.size());
        return users;
    }

    public void deleteUser(UUID userId) throws SQLException {
        logger.debug("Deleting user and their OTPs with ID: {}", userId);
        otpDao.deleteByUserId(userId);
        userDao.deleteById(userId);
        logger.debug("User with ID {} deleted successfully.", userId);
    }

    public void updateOtpConfig(int codeLength, int ttlSeconds) throws SQLException {
        logger.debug("Updating OTP config with codeLength={} and ttlSeconds={}", codeLength, ttlSeconds);
        otpConfigDao.update(codeLength, ttlSeconds);
        logger.debug("OTP configuration updated successfully.");
    }
}
