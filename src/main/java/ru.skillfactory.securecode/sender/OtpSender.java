package ru.skillfactory.securecode.sender;

public interface OtpSender {
    void sendOtp(String recipient, String code);
}
