package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.service.OtpValidationService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.UUID;

public class ValidateOtpHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ValidateOtpHandler.class);

    private final OtpValidationService validationService;

    public ValidateOtpHandler(Connection connection) {
        OtpDao otpDao = new OtpDao(connection);
        this.validationService = new OtpValidationService(otpDao);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Invalid request method: {}", exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }

        JSONObject requestJson = new JSONObject(requestBodyBuilder.toString());

        UUID userId = UUID.fromString(requestJson.getString("userId"));
        String operationId = requestJson.getString("operationId");
        String code = requestJson.getString("code");

        logger.info("Received OTP validation request for userId: {}, operationId: {}, code: {}", userId, operationId, code);

        boolean isValid = validationService.validateOtp(userId, operationId, code);

        JSONObject responseJson = new JSONObject();
        responseJson.put("valid", isValid);

        byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }

        logger.info("OTP validation result for userId {}: {}", userId, isValid ? "valid" : "invalid");
    }
}
