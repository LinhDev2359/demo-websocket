package com.wallet.entity;

public enum UserStatus {
    ACTIVE("ACTIVE", "User account is active and operational"),           // Index 0 = DB ENUM 'ACTIVE' (1st position)
    INACTIVE("INACTIVE", "User account is temporarily inactive"),         // Index 1 = DB ENUM 'INACTIVE' (2nd position)
    SUSPENDED("SUSPENDED", "User account is suspended due to policy violations"); // Index 2 = DB ENUM 'SUSPENDED' (3rd position)

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromCode(String code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status code: " + code);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canLogin() {
        return this == ACTIVE;
    }
}