package com.altitudebank;

import com.altitudebank.command.BankCommand;
import com.altitudebank.listener.ChatInputListener;
import com.altitudebank.listener.DeathLossListener;
import com.altitudebank.listener.PlayerJoinListener;
import com.altitudebank.manager.BankManager;
import com.altitudebank.manager.ChatInputManager;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.manager.VaultManager;
import com.altitudebank.placeholder.AltitudeBankPlaceholders;
import com.altitudebank.task.AutoSaveTask;
import com.altitudebank.task.WealthTaxTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AltitudeBankPlugin – entry point.
 * Boots all managers, registers listeners/commands, and starts scheduled tasks.
 */
public final class AltitudeBankPlugin extends JavaPlugin {

    // Singleton accessor — avoids passing plugin references everywhere
    private static AltitudeBankPlugin instance;

    private ConfigManager configManager;
    private VaultManager vaultManager;
    private BankManager bankManager;
    private ChatInputManager chatInputManager;

    @Override
    public void onEnable() {
        instance = this;

        // ── Config ─────────────────────────────────────────────
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // ── Vault ──────────────────────────────────────────────
        vaultManager = new VaultManager(this);
        if (!vaultManager.isReady()) {
            getLogger().severe("Vault economy not found! Plugin disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // ── Bank data ──────────────────────────────────────────
        bankManager = new BankManager(this);
        bankManager.loadAll();

        // ── Chat input manager ─────────────────────────────────
        chatInputManager = new ChatInputManager(this);

        // ── Listeners ──────────────────────────────────────────
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathLossListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // ── Commands ───────────────────────────────────────────
        BankCommand bankCommand = new BankCommand(this);
        getCommand("bank").setExecutor(bankCommand);
        getCommand("bank").setTabCompleter(bankCommand);

        // ── Scheduled tasks ─────────────────────────────────────
        long autoSaveTicks = configManager.getAutoSaveIntervalSeconds() * 20L;
        new AutoSaveTask(this).runTaskTimerAsynchronously(this, autoSaveTicks, autoSaveTicks);

        // Wealth tax uses its own persistent-time logic (stored in config/data)
        new WealthTaxTask(this).runTaskTimer(this, 20L * 60, 20L * 60); // check every minute

        // ── PlaceholderAPI ─────────────────────────────────────
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                && configManager.isPlaceholderApiEnabled()) {
            new AltitudeBankPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("AltitudeBank v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (bankManager != null) {
            bankManager.saveAll(); // synchronous final save
        }
        getLogger().info("AltitudeBank disabled. All data saved.");
    }

    // ── Static accessor ────────────────────────────────────────

    public static AltitudeBankPlugin getInstance() {
        return instance;
    }

    // ── Getters ────────────────────────────────────────────────

    public ConfigManager getConfigManager() { return configManager; }
    public VaultManager getVaultManager()   { return vaultManager; }
    public BankManager getBankManager()     { return bankManager; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
}
