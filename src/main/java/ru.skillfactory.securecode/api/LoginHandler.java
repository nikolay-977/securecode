package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.service.LoginService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class LoginHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    private final LoginService loginService;

    public LoginHandler(Connection connection) {
        this.loginService = new LoginService(connection);
        logger.info("LoginHandler initialized.");
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

        logger.info("Received login request: {}", requestBody);

        JSONObject json = new JSONObject(requestBody);
        String login = json.getString("login");
        String password = json.getString("password");

        try {
            HashMap<String, String> map = loginService.authenticate(login, password);

            String token = map.get("token");
            String userId = map.get("userId");

            if (token != null) {
                logger.info("User  {} authenticated successfully.", login);
                sendResponse(exchange, 200, new JSONObject().put("token", token).put("userId", userId).toString());
            } else {
                logger.warn("Invalid credentials for user: {}", login);
                sendResponse(exchange, 401, "{\n" +
                        "  \"status\": \"fail\",\n" +
                        "  \"message\": \"Invalid credentials.\"\n" +
                        "}");
            }
        } catch (Exception e) {
            logger.error("Error during authentication: {}", e.getMessage(), e);
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
