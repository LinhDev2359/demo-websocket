package com.wallet.entity;

public enum WalletType {
    EOS("EOS", "EOS Blockchain Wallet");        // Index 0 = DB ENUM 'EOS' (1st position)

    private final String code;
    private final String description;

    WalletType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WalletType fromCode(String code) {
        for (WalletType type : WalletType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown wallet type code: " + code);
    }

    public boolean supportsTransactions() {
        return true; // EOS wallets support transactions
    }
}