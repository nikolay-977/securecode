package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpConfigDao;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final UserDao userDao;
    private final OtpDao otpDao;
    private final OtpConfigDao otpConfigDao;

    public AdminService(UserDao userDao, OtpDao otpDao, OtpConfigDao otpConfigDao) {
        this.userDao = userDao;
        this.otpDao = otpDao;
        this.otpConfigDao = otpConfigDao;
    }

    public List<User> listUsers() throws SQLException {
        logger.info("Fetching list of users excluding admins.");
        List<User> users = userDao.findAllUsersExcludingAdmins();
        logger.info("Fetched {} users.", users.size());
        return users;
    }

    public void deleteUser(UUID userId) throws SQLException {
        logger.info("Deleting user and their OTPs with ID: {}", userId);
        otpDao.deleteByUserId(userId);
        userDao.deleteById(userId);
        logger.info("User with ID {} deleted successfully.", userId);
    }

    public void updateOtpConfig(int codeLength, int ttlSeconds) throws SQLException {
        logger.info("Updating OTP config with codeLength={} and ttlSeconds={}", codeLength, ttlSeconds);
        otpConfigDao.update(codeLength, ttlSeconds);
        logger.info("OTP configuration updated successfully.");
    }
}
