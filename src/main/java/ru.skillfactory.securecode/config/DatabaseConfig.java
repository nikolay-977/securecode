package ru.skillfactory.securecode.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    public static Connection initializeDatabase() {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                logger.debug("Unable to find db.properties file.");
                return null;
            }

            Properties prop = new Properties();
            prop.load(input);

            String url = prop.getProperty("db.url");
            String user = prop.getProperty("db.user");
            String password = prop.getProperty("db.password");

            logger.debug("Attempting to connect to the database at {}", url);
            Connection connection = DriverManager.getConnection(url, user, password);
            logger.debug("Database connection established successfully.");
            return connection;
        } catch (IOException ex) {
            logger.error("Error loading database properties: {}", ex.getMessage(), ex);
            return null;
        } catch (SQLException ex) {
            logger.error("Database connection error: {}", ex.getMessage(), ex);
            return null;
        }
    }
}
