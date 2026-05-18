package com.altitudebank.model;

import java.util.UUID;

/**
 * BankAccount — represents a single player's bank data.
 * Kept intentionally simple: balance + wealth-tax metadata.
 */
public class BankAccount {

    private final UUID uuid;
    private double balance;

    /** Epoch-milliseconds when wealth tax was last collected for this player. */
    private long lastWealthTaxTime;

    /**
     * Amount taxed while the player was offline (cleared on first join-notify).
     * 0 means no pending notification.
     */
    private double pendingWealthTaxNotification;

    public BankAccount(UUID uuid, double balance, long lastWealthTaxTime) {
        this.uuid = uuid;
        this.balance = balance;
        this.lastWealthTaxTime = lastWealthTaxTime;
        this.pendingWealthTaxNotification = 0;
    }

    // ── Getters / Setters ─────────────────────────────────────

    public UUID getUuid() { return uuid; }

    public double getBalance() { return balance; }

    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
    }

    /** Adds {@code amount} to balance, returns the new balance. */
    public double deposit(double amount) {
        balance += amount;
        return balance;
    }

    /**
     * Subtracts {@code amount} from balance, clamped to 0.
     * @return actual amount deducted
     */
    public double withdraw(double amount) {
        double actual = Math.min(amount, balance);
        balance -= actual;
        return actual;
    }

    public long getLastWealthTaxTime() { return lastWealthTaxTime; }
    public void setLastWealthTaxTime(long time) { this.lastWealthTaxTime = time; }

    public double getPendingWealthTaxNotification() { return pendingWealthTaxNotification; }
    public void setPendingWealthTaxNotification(double amount) { this.pendingWealthTaxNotification = amount; }
}
