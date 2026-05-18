package com.altitudebank.manager;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.model.ChatInputSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatInputManager — tracks which players are currently in a custom-input mode.
 * Sessions expire after {@link ChatInputSession#TIMEOUT_MS} to prevent stuck states.
 */
public class ChatInputManager {

    private final AltitudeBankPlugin plugin;
    /** Maps player UUID → active session */
    private final Map<UUID, ChatInputSession> sessions = new ConcurrentHashMap<>();

    public ChatInputManager(AltitudeBankPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers a new input session for the player, replacing any existing one. */
    public void startSession(UUID uuid, ChatInputSession.Type type) {
        sessions.put(uuid, new ChatInputSession(type));
    }

    /** @return the active (non-expired) session, or null */
    public ChatInputSession getSession(UUID uuid) {
        ChatInputSession session = sessions.get(uuid);
        if (session == null) return null;
        if (session.isExpired()) {
            sessions.remove(uuid);
            return null;
        }
        return session;
    }

    /** @return true if the player has an active session */
    public boolean hasSession(UUID uuid) {
        return getSession(uuid) != null;
    }

    /** Removes the session (called after successful processing or cancellation). */
    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
    }
}
