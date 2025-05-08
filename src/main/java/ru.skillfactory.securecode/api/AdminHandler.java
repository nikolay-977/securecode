package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.dao.OtpConfigDao;
import ru.skillfactory.securecode.dao.OtpDao;
import ru.skillfactory.securecode.dao.UserDao;
import ru.skillfactory.securecode.model.User;
import ru.skillfactory.securecode.service.AdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AdminHandler.class);
    private final UserDao userDao;
    private final OtpDao otpDao;
    private final OtpConfigDao otpConfigDao;
    private final AdminService adminService;

    public AdminHandler(Connection connection) {
        this.userDao = new UserDao(connection);
        this.otpDao = new OtpDao(connection);
        this.otpConfigDao = new OtpConfigDao(connection);
        this.adminService = new AdminService(userDao, otpDao, otpConfigDao);
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

        logger.info("Received request: {}", requestBody);

        JSONObject json = new JSONObject(requestBody);
        String action = json.getString("action");

        try {
            switch (action) {
                case "updateOtpConfig":
                    updateOtpConfig(json);
                    sendResponse(exchange, 200, "{\n" +
                            "  \"status\": \"success\",\n" +
                            "  \"message\": \"OTP configuration updated successfully.\"\n" +
                            "}");
                    break;
                case "listUsers":
                    listUsers(exchange);
                    break;
                case "deleteUser":
                    deleteUser(json);
                    sendResponse(exchange, 204, "{\n" +
                            "  \"status\": \"success\",\n" +
                            "  \"message\": \"User deleted successfully.\"\n" +
                            "}");
                    break;
                default:
                    sendResponse(exchange, 400, "{\n" +
                            "  \"status\": \"fail\",\n" +
                            "  \"message\": \"Invalid action.\"\n" +
                            "}");
            }
        } catch (SQLException e) {
            logger.error("Database error: {}", e.getMessage(), e);
            sendResponse(exchange, 500, "{\n" +
                    "  \"status\": \"fail\",\n" +
                    "  \"message\": \"Database error.\"\n" +
                    "}");
        }
    }

    private void deleteUser(JSONObject json) throws SQLException {
        UUID userId = UUID.fromString(json.getString("userId"));
        adminService.deleteUser(userId);
        logger.info("Deleted user with ID: {}", userId);
    }

    private void updateOtpConfig(JSONObject json) throws SQLException {
        int codeLength = json.getInt("codeLength");
        int ttlSeconds = json.getInt("ttlSeconds");
        adminService.updateOtpConfig(codeLength, ttlSeconds);
        logger.info("Updated config: codeLength: {}, ttlSeconds: {}", codeLength, ttlSeconds);
    }

    private void listUsers(HttpExchange exchange) throws SQLException, IOException {
        List<User> users = adminService.listUsers();
        logger.info("Received list of users: siz: {}", users.size());

        JSONArray jsonArray = new JSONArray();
        for (User user : users) {
            JSONObject userJson = new JSONObject();
            userJson.put("id", user.id.toString());
            userJson.put("login", user.login);
            userJson.put("phone", user.phone);
            userJson.put("email", user.email);
            jsonArray.put(userJson);
        }
        sendResponse(exchange, 200, jsonArray.toString());
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
