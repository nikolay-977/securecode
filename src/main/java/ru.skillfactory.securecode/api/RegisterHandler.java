package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.service.RegisterService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class RegisterHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RegisterHandler.class);

    private final RegisterService registerService;

    public RegisterHandler(Connection connection) {
        this.registerService = new RegisterService(connection);
        logger.info("RegisterHandler initialized.");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            logger.warn("Invalid request method: {}", exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String requestBody = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
        ).lines().collect(Collectors.joining("\n"));

        logger.info("Received register request: {}", requestBody);

        JSONObject json = new JSONObject(requestBody);
        String login = json.getString("login");
        String password = json.getString("password");
        String role = json.getString("role");
        String phone = json.getString("phone");
        String email = json.getString("email");
        String telegramId = json.getString("telegramId");

        try {
            boolean success = registerService.registerUser(login, password, role, phone, email, telegramId);
            if (!success) {
                logger.warn("Registration failed: Admin already exists for login: {}", login);
                sendResponse(exchange, 400, "{\n" +
                        "  \"status\": \"fail\",\n" +
                        "  \"message\": \"Admin already exists.\"\n" +
                        "}");
                return;
            }
            logger.info("User  registered successfully: {}", login);
            sendResponse(exchange, 200, "{\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"message\": \"User registered successfully.\"\n" +
                    "}");
        } catch (SQLException e) {
            logger.error("Database error during registration for user {}: {}", login, e.getMessage(), e);
            sendResponse(exchange, 500, "{\n" +
                    "  \"status\": \"fail\",\n" +
                    "  \"message\": \"Database error.\"\n" +
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
