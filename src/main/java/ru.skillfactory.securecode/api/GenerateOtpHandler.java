package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.model.OtpCode;
import ru.skillfactory.securecode.model.User;
import ru.skillfactory.securecode.service.AuthenticationService;
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
    private final AuthenticationService authenticationService;

    public GenerateOtpHandler(Connection connection) {
        this.otpService = new OtpService(connection);
        this.senderService = new SenderService(connection);
        this.otpExpirationService = new OtpExpirationService(connection);
        this.otpExpirationService.start();
        this.authenticationService = new AuthenticationService(connection);
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

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String requestBody = reader.lines().collect(Collectors.joining("\n"));
                logger.debug("Request body: {}", requestBody);

                JSONObject json = new JSONObject(requestBody);
                UUID userId = UUID.fromString(json.getString("userId"));

                // Проверка соответствия токена и userId
                if (!userId.equals(authUser.id)) {
                    logger.warn("Access denied: token userId {} does not match requested userId {}", authUser.id, userId);
                    sendResponse(exchange, 403, "{\"message\": \"Forbidden: Access denied.\"}");
                    return;
                }

                String operationId = json.optString("operationId", UUID.randomUUID().toString());
                logger.info("Generating OTP for userId: {}, operationId: {}", userId, operationId);

                OtpCode otpCode = otpService.generateAndPersistOtpWithFile(userId, operationId);

                JSONObject response = new JSONObject();
                response.put("message", "OTP code generated and saved to file.");
                response.put("operationId", otpCode.operationId);

                sendResponse(exchange, 200, response.toString());

                logger.info("Sending OTP to userId: {}", userId);
                senderService.sendOtp(userId, otpCode.code);
            }

        } catch (Exception e) {
            logger.error("Error handling OTP generation request", e);
            sendResponse(exchange, 500, "{ \"status\": \"fail\", \"message\": \"Internal server error.\" }");
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
