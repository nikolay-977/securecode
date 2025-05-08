package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpConfigDao;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.OtpCode;
import ru.skillfactory.securecode.service.OtpExpirationService;
import ru.skillfactory.securecode.service.OtpService;
import ru.skillfactory.securecode.service.SenderService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.UUID;
import java.util.stream.Collectors;

public class GenerateOtpHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(GenerateOtpHandler.class);

    private final OtpService otpService;
    private final SenderService senderService;
    private final OtpExpirationService otpExpirationService;

    public GenerateOtpHandler(Connection connection) {
        OtpDao otpDao = new OtpDao(connection);
        UserDao userDao = new UserDao(connection);
        OtpConfigDao otpConfigDao = new OtpConfigDao(connection);
        this.otpService = new OtpService(otpDao, otpConfigDao);
        this.senderService = new SenderService(userDao);
        this.otpExpirationService = new OtpExpirationService(otpDao);
        this.otpExpirationService.start();
        logger.info("GenerateOtpHandler initialized.");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Received request: {}", exchange.getRequestURI());

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            logger.warn("Unsupported HTTP method: {}", exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String requestBody = reader.lines().collect(Collectors.joining("\n"));
            logger.debug("Request body: {}", requestBody);

            JSONObject json = new JSONObject(requestBody);
            UUID userId = UUID.fromString(json.getString("userId"));
            String operationId = json.optString("operationId", UUID.randomUUID().toString());

            logger.info("Generating OTP for userId: {}, operationId: {}", userId, operationId);

            OtpCode otpCode = otpService.generateAndPersistOtpWithFile(userId, operationId);

            JSONObject response = new JSONObject();
            response.put("message", "OTP code generated and saved to file.");
            response.put("operationId", otpCode.operationId);

            sendResponse(exchange, 200, response.toString());

            logger.info("Sending OTP to userId: {}", userId);
            senderService.sendOtp(userId, otpCode.code);
        } catch (Exception e) {
            logger.error("Error handling OTP generation request", e);
            sendResponse(exchange, 500, "{\n" +
                    "  \"status\": \"fail\",\n" +
                    "  \"message\": \"Internal server error.\"\n" +
                    "}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        logger.debug("Sending response with status {}: {}", statusCode, message);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
