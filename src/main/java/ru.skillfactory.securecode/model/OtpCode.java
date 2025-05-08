package ru.skillfactory.securecode.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class OtpCode {
    public UUID id;
    public UUID userId;
    public String code;
    public String status; // ACTIVE, EXPIRED, USED
    public String operationId;
    public LocalDateTime createdAt;
    public LocalDateTime expiresAt;
}
