package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;
import ru.skillfactory.securecode.sender.EmailSender;
import ru.skillfactory.securecode.sender.OtpSender;
import ru.skillfactory.securecode.sender.SmsSender;
import ru.skillfactory.securecode.sender.TelegramSender;

import java.sql.SQLException;
import java.util.UUID;

public class SenderService {
    private static final Logger logger = LoggerFactory.getLogger(SenderService.class);
    private final UserDao userDao;

    public SenderService(UserDao userDao) {
        this.userDao = userDao;
        logger.info("SenderService initialized");
    }

    public void sendOtp(UUID userId, String code) {
        User user;
        try {
            logger.info("Attempting to send OTP for userId={}", userId);
            user = userDao.findById(userId);

            logger.info("User found: email={}, phone={}, telegramId={}", user.email, user.phone, user.telegramId);

            sendEmail(user.email, code);
            smsSend(user.phone, code);
            sendTelegram(user.telegramId, code);

            logger.info("OTP sent successfully for userId={}", userId);
        } catch (SQLException e) {
            logger.error("Failed to retrieve user with userId={}. Error: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendEmail(String email, String code) {
        try {
            logger.debug("Sending OTP via Email to {}", email);
            OtpSender otpSender = new EmailSender();
            otpSender.sendOtp(email, code);
            logger.debug("OTP sent successfully via Email to {}", email);
        } catch (Exception e) {
            logger.error("Error sending OTP via Email to {}: {}", email, e.getMessage());
        }
    }

    private void smsSend(String phone, String code) {
        try {
            logger.debug("Sending OTP via SMS to {}", phone);
            OtpSender otpSender = new SmsSender();
            otpSender.sendOtp(phone, code);
            logger.debug("OTP sent successfully via SMS to {}", phone);
        } catch (Exception e) {
            logger.error("Error sending OTP via SMS to {}: {}", phone, e.getMessage());
        }
    }

    private void sendTelegram(String telegramId, String code) {
        try {
            logger.debug("Sending OTP via Telegram to {}", telegramId);
            OtpSender otpSender = new TelegramSender();
            otpSender.sendOtp(telegramId, code);
            logger.debug("OTP sent successfully via Telegram to {}", telegramId);
        } catch (Exception e) {
            logger.error("Error sending OTP via Telegram to {}: {}", telegramId, e.getMessage());
        }
    }
}
