package com.nero.transferguard.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class Account {
    private final String id;
    private BigDecimal balance;

    public Account(String id, BigDecimal openingBalance) {
        this.id = Objects.requireNonNull(id);
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance must be zero or greater");
        }
        this.balance = openingBalance;
    }

    public String id() {
        return id;
    }

    public BigDecimal balance() {
        return balance;
    }

    public void debit(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
