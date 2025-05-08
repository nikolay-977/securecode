package ru.skillfactory.securecode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.model.OtpCode;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class OtpValidationService {
    private static final Logger logger = LoggerFactory.getLogger(OtpValidationService.class);
    private final OtpDao otpDao;

    public OtpValidationService(Connection connection) {
        this.otpDao = new OtpDao(connection);
        logger.debug("OtpValidationService initialized");
    }

    public boolean validateOtp(UUID userId, String operationId, String code) {
        logger.info("Validating OTP for userId={}, operationId={}", userId, operationId);
        Optional<OtpCode> otpOpt = otpDao.findActiveByUserAndOperation(userId, operationId);

        if (otpOpt.isEmpty()) {
            logger.warn("No active OTP found for userId={}, operationId={}", userId, operationId);
            return false;
        }

        OtpCode otp = otpOpt.get();

        if (!otp.code.equals(code)) {
            logger.warn("Invalid OTP code for userId={}, operationId={}", userId, operationId);
            return false;
        }

        if (otp.expiresAt.isBefore(LocalDateTime.now())) {
            otpDao.updateStatus(otp.id, "EXPIRED");
            logger.warn("OTP expired for userId={}, operationId={}", userId, operationId);
            return false;
        }

        otpDao.updateStatus(otp.id, "USED");
        logger.debug("OTP validated successfully for userId={}, operationId={}", userId, operationId);
        return true;
    }
}
