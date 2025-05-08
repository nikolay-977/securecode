package ru.skillfactory.securecode.api;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;

public class HttpServerInitializer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerInitializer.class);
    private static Connection connection;

    public static void setConnection(Connection conn) {
        connection = conn;
        logger.info("Database connection has been set.");
    }

    public static void startServer() {
        try {
            logger.info("Attempting to start HTTP server on port 8000...");
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            server.createContext("/register", new RegisterHandler(connection));
            logger.info("Registered /register endpoint");

            server.createContext("/login", new LoginHandler(connection));
            logger.info("Registered /login endpoint");

            server.createContext("/otp/generate", new GenerateOtpHandler(connection));
            logger.info("Registered /otp/generate endpoint");

            server.createContext("/otp/validate", new ValidateOtpHandler(connection));
            logger.info("Registered /otp/validate endpoint");

            server.createContext("/admin", new AdminHandler(connection));
            logger.info("Registered /admin endpoint");

            server.setExecutor(null);
            server.start();
            logger.info("Server started successfully at http://localhost:8000");
        } catch (IOException e) {
            logger.error("Failed to start HTTP server", e);
        }
    }
}
