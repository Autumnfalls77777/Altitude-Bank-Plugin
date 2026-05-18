package com.altitudebank.task;

import com.altitudebank.AltitudeBankPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * AutoSaveTask — periodically saves all bank data to disk.
 * Runs asynchronously to avoid blocking the main thread.
 */
public class AutoSaveTask extends BukkitRunnable {

    private final AltitudeBankPlugin plugin;

    public AutoSaveTask(AltitudeBankPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // asyncSave() handles file I/O; ConcurrentHashMap is safe for async read
        plugin.getBankManager().asyncSave();
        plugin.getLogger().fine("Auto-save complete.");
    }
}
