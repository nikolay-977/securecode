package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.SessionDao;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserDao userDao;
    private final SessionDao sessionDao;

    public AuthenticationService(Connection connection) {
        this.userDao = new UserDao(connection);
        this.sessionDao = new SessionDao(connection);
    }

    public void saveSession(String token, UUID userId) throws SQLException {
        sessionDao.saveSession(token, userId);
        logger.info("Saved session for userId: {}", userId);
    }

    public boolean isValidAdminToken(String token) {
        User user = findUserByToken(token);
        return user != null && "ADMIN".equalsIgnoreCase(user.role);
    }

    public User findUserByToken(String token) {
        UUID userId = null;
        try {
            userId = sessionDao.findUserIdByToken(token);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (userId != null) {
            User user = null;
            try {
                user = userDao.findById(userId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            logger.info("User found by token: {}", user.login);
            return user;
        } else {
            logger.warn("Invalid token: user not found");
            return null;
        }
    }

    public void logout(String token) throws SQLException {
        sessionDao.deleteSession(token);
        logger.info("Session deleted for token: {}", token);
    }
}
