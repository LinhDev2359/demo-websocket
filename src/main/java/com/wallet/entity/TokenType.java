package com.wallet.entity;

public enum TokenType {
    A("A", "A token type"),           // Index 0 = DB ENUM 'A' (1st position)
    RAM("ram", "RAM token"),          // Index 1 = DB ENUM 'ram' (2nd position)
    RAMS("rams", "RAMS token"),       // Index 2 = DB ENUM 'rams' (3rd position)
    WRAM("wram", "WRAM token");       // Index 3 = DB ENUM 'wram' (4th position)

    private final String symbol;
    private final String description;

    TokenType(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    public static TokenType fromSymbol(String symbol) {
        for (TokenType type : TokenType.values()) {
            if (type.symbol.equalsIgnoreCase(symbol)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown token symbol: " + symbol);
    }
}