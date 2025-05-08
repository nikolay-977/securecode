package ru.skillfactory.securecode.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.securecode.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);
    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public void register(User user) throws SQLException {
        logger.debug("Registering user: {}", user.login);
        String sql = "INSERT INTO public.users (login, password_hash, role, phone, email, telegram_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.login);
            stmt.setString(2, user.passwordHash);
            stmt.setString(3, user.role);
            stmt.setString(4, user.phone);
            stmt.setString(5, user.email);
            stmt.setString(6, user.telegramId);
            stmt.executeUpdate();
            logger.debug("Successfully registered new user with login: {}", user.login);
        } catch (SQLException e) {
            logger.error("Error registering user {}: {}", user.login, e.getMessage(), e);
            throw e;
        }
    }

    public User findByLogin(String login) throws SQLException {
        logger.debug("Finding user by login: {}", login);
        String sql = "SELECT * FROM public.users WHERE login = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = mapUserFromResultSet(rs);
                logger.info("User found by login: {}", login);
                return user;
            } else {
                logger.warn("No user found with login: {}", login);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by login {}: {}", login, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    public User findById(UUID id) throws SQLException {
        logger.debug("Finding user by ID: {}", id);
        String sql = "SELECT * FROM public.users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id, java.sql.Types.OTHER);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = mapUserFromResultSet(rs);
                logger.debug("User found by ID: {}", id);
                return user;
            } else {
                logger.warn("No user found with ID: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
        return null;
    }

    public boolean isAdminExists() throws SQLException {
        logger.debug("Checking if admin exists");
        String sql = "SELECT COUNT(*) FROM public.users WHERE role = 'ADMIN'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean exists = rs.getInt(1) > 0;
                logger.debug("Admin existence check result: {}", exists);
                return exists;
            }
        } catch (SQLException e) {
            logger.error("Error checking admin existence: {}", e.getMessage(), e);
            throw e;
        }
        return false;
    }

    public List<User> findAllUsersExcludingAdmins() throws SQLException {
        logger.debug("Finding all users excluding admins");
        String sql = "SELECT * FROM public.users WHERE role != 'ADMIN'";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapUserFromResultSet(rs));
            }
            logger.debug("Found {} users excluding admins", users.size());
        } catch (SQLException e) {
            logger.error("Error finding users excluding admins: {}", e.getMessage(), e);
            throw e;
        }
        return users;
    }

    public void deleteById(UUID id) throws SQLException {
        logger.debug("Deleting user by ID: {}", id);
        String sql = "DELETE FROM public.users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id, java.sql.Types.OTHER);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                logger.debug("User with ID {} deleted successfully", id);
            } else {
                logger.warn("No user found to delete with ID: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error deleting user by ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    private User mapUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.id = UUID.fromString(rs.getString("id"));
        user.login = rs.getString("login");
        user.passwordHash = rs.getString("password_hash");
        user.role = rs.getString("role");
        user.phone = rs.getString("phone");
        user.email = rs.getString("email");
        user.telegramId = rs.getString("telegram_id");
        return user;
    }

    public User findByToken(String token) throws SQLException {
        String sql = "SELECT u.* FROM users u " +
                "JOIN sessions s ON u.id = s.user_id " +
                "WHERE s.token = ? AND s.created_at > NOW() - INTERVAL '1 DAY'"; // Токен действителен 1 день

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.id = UUID.fromString(rs.getString("id"));
                    user.login = rs.getString("login");
                    user.passwordHash = rs.getString("password_hash");
                    user.role = rs.getString("role");
                    user.phone = rs.getString("phone");
                    user.email = rs.getString("email");
                    user.telegramId = rs.getString("telegram_id");
                    return user;
                }
            }
        }
        return null;
    }
}
