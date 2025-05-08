package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.model.User;
import ru.skillfactory.securecode.service.AuthenticationService;
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
    private final AuthenticationService authenticationService;

    public ValidateOtpHandler(Connection connection) {
        this.validationService = new OtpValidationService(connection);
        this.authenticationService = new AuthenticationService(connection);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Received OTP validation request: {}", exchange.getRequestURI());

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Invalid request method: {}", exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header");
                sendResponse(exchange, 401, "{\"message\": \"Unauthorized\"}");
                return;
            }

            String token = authHeader.substring("Bearer ".length());
            User authUser = authenticationService.findUserByToken(token);
            if (authUser == null) {
                logger.warn("Invalid token: {}", token);
                sendResponse(exchange, 401, "{\"message\": \"Unauthorized\"}");
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

            logger.info("Validating OTP for userId: {}, operationId: {}", userId, operationId);

            if (!authUser.id.equals(userId)) {
                logger.warn("Access denied: token userId {} does not match requested userId {}", authUser.id, userId);
                sendResponse(exchange, 403, "{\"message\": \"Forbidden: Access denied.\"}");
                return;
            }

            boolean isValid = validationService.validateOtp(userId, operationId, code);

            JSONObject responseJson = new JSONObject();
            responseJson.put("valid", isValid);

            sendResponse(exchange, 200, responseJson.toString());
        } catch (Exception e) {
            logger.error("Error validating OTP", e);
            sendResponse(exchange, 500, "{ \"message\": \"Internal server error\" }");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        logger.debug("Sending response: {} - {}", statusCode, message);
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
