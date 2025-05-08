package ru.skillfactory.securecode.sender;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramSender implements OtpSender {
    private static final Logger logger = LoggerFactory.getLogger(TelegramSender.class);

    private final String botToken;
    private final String chatId;
    private final String telegramApiUrl;
    private final boolean enabled;

    public TelegramSender() {
        logger.debug("Initializing TelegramSender");
        Properties config = loadConfig();
        this.botToken = config.getProperty("bot.token");
        this.chatId = config.getProperty("chat.id");
        this.telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        this.enabled = Boolean.parseBoolean(config.getProperty("enabled"));

        logger.info("TelegramSender initialized. Enabled: {}, Chat ID: {}", enabled, chatId);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Properties loadConfig() {
        try {
            logger.debug("Loading Telegram configuration from 'telegram.properties'");
            Properties props = new Properties();
            props.load(TelegramSender.class.getClassLoader().getResourceAsStream("telegram.properties"));
            logger.info("Telegram configuration loaded successfully");
            return props;
        } catch (Exception e) {
            logger.error("Failed to load Telegram configuration", e);
            throw new RuntimeException("Failed to load Telegram configuration", e);
        }
    }

    private void sendTelegramRequest(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    logger.error("Telegram API returned non-OK status: {}", statusCode);
                } else {
                    logger.info("Telegram message sent successfully");
                }
            }
        } catch (IOException e) {
            logger.error("Error sending Telegram message", e);
        }
    }

    @Override
    public void sendOtp(String destination, String code) {
        if (!enabled) {
            logger.warn("Telegram sending is disabled. Skipping OTP to {}", destination);
            return;
        }

        String message = String.format(destination + ", your confirmation code is: %s", code);
        String url = String.format("%s?chat_id=%s&text=%s", telegramApiUrl, chatId, urlEncode(message));
        logger.debug("Sending Telegram OTP message to {} via URL: {}", destination, url);
        sendTelegramRequest(url);
    }
}
