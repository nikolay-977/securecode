package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpConfigDao;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.model.OtpCode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import static ru.skillfactory.securecode.config.Config.CODE_LENGTH;
import static ru.skillfactory.securecode.config.Config.TTL_SECONDS;

public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final Random random = new Random();
    private final OtpDao otpDao;
    private final OtpConfigDao otpConfigDao;
    private int codeLength;
    private long ttlSeconds;

    public OtpService(OtpDao otpDao, OtpConfigDao otpConfigDao) {
        this.otpDao = otpDao;
        this.otpConfigDao = otpConfigDao;
        logger.info("OtpService initialized");
    }

    private void loadConfig() {
        try {
            logger.debug("Loading OTP configuration...");
            var config = otpConfigDao.get();
            if (config != null) {
                codeLength = config.codeLength;
                ttlSeconds = config.ttlSeconds;
                logger.info("OTP config loaded from DB: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
            } else {
                codeLength = CODE_LENGTH;
                ttlSeconds = TTL_SECONDS;
                logger.warn("OTP config not found in DB, using defaults from file");
            }
        } catch (SQLException e) {
            logger.error("Failed to load OTP config from DB, falling back to file", e);
            codeLength = CODE_LENGTH;
            ttlSeconds = TTL_SECONDS;
        }
    }

    public OtpCode generateOtp(UUID userId, String operationId) {
        loadConfig();
        String code = String.format("%0" + codeLength + "d", random.nextInt((int) Math.pow(10, codeLength)));
        LocalDateTime now = LocalDateTime.now();

        OtpCode otp = new OtpCode();
        otp.userId = userId;
        otp.code = code;
        otp.status = "ACTIVE";
        otp.operationId = operationId;
        otp.createdAt = now;
        otp.expiresAt = now.plusSeconds(ttlSeconds);

        logger.info("Generated OTP for userId={}, operationId={}", userId, operationId);
        return otp;
    }

    public OtpCode generateAndPersistOtpWithFile(UUID userId, String operationId) {
        OtpCode otp = generateOtp(userId, operationId);
        otpDao.save(otp);
        logger.info("Persisted OTP for userId={}, operationId={}", userId, operationId);
        saveCodeToFile(otp.userId, otp.operationId, otp.code);
        return otp;
    }

    private void saveCodeToFile(UUID userId, String operationId, String code) {
        String filename = userId + "_" + operationId + ".otp.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("OTP Code: " + code);
            logger.info("Saved OTP code to file: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to save OTP code to file: {}", filename, e);
        }
    }
}
