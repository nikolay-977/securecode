package ru.skillfactory.securecode.service;

import ru.skillfactory.securecode.dao.OtpDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.skillfactory.securecode.config.Config.CHECK_INTERVALS_SECONDS;

public class OtpExpirationService {
    private final OtpDao otpDao;
    private final ScheduledExecutorService scheduler;
    private final long checkIntervalSeconds;

    public OtpExpirationService(OtpDao otpDao) {
        this.otpDao = otpDao;
        this.checkIntervalSeconds = CHECK_INTERVALS_SECONDS;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAndExpireOtpCodes, 0, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    private void checkAndExpireOtpCodes() {
        otpDao.expireOldOtps();
    }
}
