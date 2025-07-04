package com.wallet.entity;

public enum WalletStatus {
    ACTIVE("ACTIVE", "Active and operational"),      // Index 0 = DB ENUM 'ACTIVE' (1st position)
    INACTIVE("INACTIVE", "Temporarily disabled"),    // Index 1 = DB ENUM 'INACTIVE' (2nd position)  
    DELETED("DELETED", "Soft deleted");              // Index 2 = DB ENUM 'DELETED' (3rd position)

    private final String code;
    private final String description;

    WalletStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WalletStatus fromCode(String code) {
        for (WalletStatus status : WalletStatus.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown wallet status code: " + code);
    }

    public boolean isOperational() {
        return this == ACTIVE;
    }

    public boolean canReceiveTransactions() {
        return this == ACTIVE || this == INACTIVE;
    }

    public boolean canSendTransactions() {
        return this == ACTIVE;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }
}