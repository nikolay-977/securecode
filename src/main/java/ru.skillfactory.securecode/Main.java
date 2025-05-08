package ru.skillfactory.securecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.api.HttpServerInitializer;
import ru.skillfactory.securecode.config.DatabaseConfig;

import java.sql.Connection;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting application...");

        Connection connection = DatabaseConfig.initializeDatabase();

        if (connection != null) {
            logger.info("Database connection established successfully.");
            HttpServerInitializer.setConnection(connection);
            HttpServerInitializer.startServer();
            logger.info("HTTP Server started successfully on port 8000.");
        } else {
            logger.error("Failed to connect to the database.");
        }
    }
}
