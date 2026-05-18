package com.altitudebank.manager;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.model.BankAccount;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * BankManager — owns all bank accounts.
 *
 * Thread-safety strategy:
 *   - ConcurrentHashMap used for cache (safe for concurrent reads + single-writer)
 *   - saveAll() / loadAll() are called synchronously on main thread at startup/shutdown
 *   - asyncSave() is used during auto-save (writes a snapshot to disk off-thread)
 *   - No balance mutations happen off the main thread, so no additional locking needed
 *     for balance fields.
 */
public class BankManager {

    private static final String DATA_FILE = "bankdata.yml";

    private final AltitudeBankPlugin plugin;
    private final File dataFile;

    /** In-memory cache: UUID → BankAccount */
    private final ConcurrentHashMap<UUID, BankAccount> accounts = new ConcurrentHashMap<>();

    public BankManager(AltitudeBankPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), DATA_FILE);
    }

    // ── Persistence ───────────────────────────────────────────

    /** Loads all accounts from bankdata.yml into memory. Called once on enable. */
    public void loadAll() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No bank data file found; starting fresh.");
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.contains("accounts")) return;

        for (String key : data.getConfigurationSection("accounts").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance = data.getDouble("accounts." + key + ".balance", 0);
                long lastTax   = data.getLong("accounts." + key + ".lastWealthTax", System.currentTimeMillis());
                double pending = data.getDouble("accounts." + key + ".pendingTaxNotify", 0);

                BankAccount account = new BankAccount(uuid, balance, lastTax);
                account.setPendingWealthTaxNotification(pending);
                accounts.put(uuid, account);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid UUID in bank data: " + key);
            }
        }
        plugin.getLogger().info("Loaded " + accounts.size() + " bank accounts.");
    }

    /** Saves all accounts to bankdata.yml. Safe to call synchronously. */
    public void saveAll() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, BankAccount> entry : accounts.entrySet()) {
            String key = "accounts." + entry.getKey().toString();
            BankAccount acct = entry.getValue();
            data.set(key + ".balance", acct.getBalance());
            data.set(key + ".lastWealthTax", acct.getLastWealthTaxTime());
            data.set(key + ".pendingTaxNotify", acct.getPendingWealthTaxNotification());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save bank data!", e);
        }
    }

    /**
     * Asynchronous save — copies current state to a snapshot map then writes to disk.
     * Call this from a BukkitRunnable that is already async.
     */
    public void asyncSave() {
        // Build a snapshot on the calling thread (still async; map is ConcurrentHashMap)
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, BankAccount> entry : accounts.entrySet()) {
            String key = "accounts." + entry.getKey().toString();
            BankAccount acct = entry.getValue();
            data.set(key + ".balance", acct.getBalance());
            data.set(key + ".lastWealthTax", acct.getLastWealthTaxTime());
            data.set(key + ".pendingTaxNotify", acct.getPendingWealthTaxNotification());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Async save failed!", e);
        }
    }

    // ── Account access ────────────────────────────────────────

    /**
     * Gets or creates a BankAccount for the given UUID.
     * Never returns null.
     */
    public BankAccount getOrCreate(UUID uuid) {
        return accounts.computeIfAbsent(uuid, id -> {
            double startBalance = plugin.getConfigManager().getStartingBalance();
            return new BankAccount(id, startBalance, System.currentTimeMillis());
        });
    }

    /** Returns account or null if it doesn't exist in cache. */
    public BankAccount get(UUID uuid) {
        return accounts.get(uuid);
    }

    /**
     * Deposits {@code amount} into the player's bank.
     * @return new bank balance
     * @throws IllegalArgumentException if amount <= 0
     */
    public double deposit(UUID uuid, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        BankAccount acct = getOrCreate(uuid);
        return acct.deposit(amount);
    }

    /**
     * Withdraws {@code amount} from the player's bank.
     * @return actual amount withdrawn (may be less than requested if balance is low)
     */
    public double withdraw(UUID uuid, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        BankAccount acct = getOrCreate(uuid);
        return acct.withdraw(amount);
    }

    /** @return current bank balance for this UUID (0 if account doesn't exist) */
    public double getBalance(UUID uuid) {
        BankAccount acct = accounts.get(uuid);
        return acct == null ? 0 : acct.getBalance();
    }

    /** Direct balance setter — used by wealth tax. */
    public void setBalance(UUID uuid, double balance) {
        getOrCreate(uuid).setBalance(balance);
    }

    /** Exposes the live account map for iteration (wealth tax task needs this). */
    public ConcurrentHashMap<UUID, BankAccount> getAllAccounts() {
        return accounts;
    }
}
