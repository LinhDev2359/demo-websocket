package com.wallet.entity;

public enum BalanceStatus {
    ACTIVE("ACTIVE", "Balance is active and synchronized"),
    SYNCING("SYNCING", "Balance is being synchronized"),
    STALE("STALE", "Balance data is outdated"),
    ERROR("ERROR", "Error in balance synchronization"),
    FROZEN("FROZEN", "Balance is frozen due to security");

    private final String code;
    private final String description;

    BalanceStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static BalanceStatus fromCode(String code) {
        for (BalanceStatus status : BalanceStatus.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown balance status code: " + code);
    }

    public boolean isReliable() {
        return this == ACTIVE;
    }

    public boolean needsSync() {
        return this == STALE || this == ERROR;
    }

    public boolean isSyncing() {
        return this == SYNCING;
    }

    public boolean hasError() {
        return this == ERROR;
    }

    public boolean isFrozen() {
        return this == FROZEN;
    }

    public boolean canBeUsedForTransactions() {
        return this == ACTIVE && !isFrozen();
    }
}