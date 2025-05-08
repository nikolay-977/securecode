package ru.skillfactory.securecode.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import static ru.skillfactory.securecode.config.Config.EXPIRATION_TIME;

public class LoginService {
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    private final UserDao userDao;
    private final SecretKey secretKey;

    public LoginService(UserDao userDao) {
        this.userDao = userDao;
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        logger.info("LoginService initialized with secure JWT key.");
    }

    public HashMap<String, String> authenticate(String login, String password) throws Exception {
        logger.info("Authenticating user with login: {}", login);
        User user = userDao.findByLogin(login);
        if (user != null && user.passwordHash.equals(hashPassword(password))) {
            logger.info("Authentication successful for user: {}", login);
            HashMap<String, String> map = new HashMap<>();
            String token = generateToken(user);
            map.put("token", token);
            map.put("userId", user.id.toString());
            return map;
        } else {
            logger.warn("Authentication failed for user: {}", login);
        }
        return null;
    }

    private String generateToken(User user) {
        logger.debug("Generating JWT for user: {}", user.login);
        return Jwts.builder()
                .setSubject(user.login)
                .claim("role", user.role)
                .setExpiration(Date.from(Instant.now().plusMillis(EXPIRATION_TIME)))
                .signWith(secretKey)
                .compact();
    }

    private String hashPassword(String password) {
        try {
            logger.debug("Hashing password...");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Password hashing failed", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
