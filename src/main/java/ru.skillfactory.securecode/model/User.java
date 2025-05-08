package ru.skillfactory.securecode.model;

import java.util.UUID;

public class User {
    public UUID id;
    public String login;
    public String passwordHash;
    public String role; // ADMIN or USER
    public String phone;
    public String email;
    public String telegramId;

}
