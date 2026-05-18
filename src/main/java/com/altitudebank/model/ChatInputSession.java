package com.altitudebank.model;

/**
 * Represents an active chat-input session for a player.
 * Used by depositcustom / withdrawcustom commands.
 */
public class ChatInputSession {

    public enum Type { DEPOSIT, WITHDRAW }

    private final Type type;
    /** System time (ms) when this session was created — used for timeout. */
    private final long createdAt;
    /** Sessions expire after 60 seconds of inactivity. */
    public static final long TIMEOUT_MS = 60_000;

    public ChatInputSession(Type type) {
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public Type getType() { return type; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > TIMEOUT_MS;
    }
}
