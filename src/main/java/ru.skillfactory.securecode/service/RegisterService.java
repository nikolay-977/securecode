package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class RegisterService {
    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);
    private final UserDao userDao;

    public RegisterService(UserDao userDao) {
        this.userDao = userDao;
        logger.info("RegisterService initialized");
    }

    public boolean registerUser(String login, String password, String role, String phone, String email, String telegramId) throws SQLException {
        logger.info("Attempting to register user with login={}", login);

        if ("ADMIN".equalsIgnoreCase(role) && userDao.isAdminExists()) {
            logger.warn("Admin user already exists, registration attempt blocked for login={}", login);
            return false;
        }

        User user = new User();
        user.login = login;
        user.passwordHash = hashPassword(password);
        user.role = role.toUpperCase();
        user.phone = phone;
        user.email = email;
        user.telegramId = telegramId;

        userDao.register(user);
        logger.info("User registered successfully with login={}", login);
        return true;
    }

    private String hashPassword(String password) {
        try {
            logger.debug("Hashing password for security");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing password: {}", e.getMessage(), e);
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
