package com.altitudebank.task;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.manager.BankManager;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.model.BankAccount;
import com.altitudebank.util.MessageUtil;
import com.altitudebank.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

/**
 * WealthTaxTask — runs every minute (main thread) and checks each account to see
 * whether their configured interval has elapsed since their last wealth tax collection.
 *
 * Per-player last-tax timestamps allow different players to be taxed at different times
 * (e.g. a player who joined mid-cycle won't be taxed immediately).
 *
 * This approach is restart-safe: timestamps are persisted in bankdata.yml.
 */
public class WealthTaxTask extends BukkitRunnable {

    private final AltitudeBankPlugin plugin;
    private final BankManager bankManager;
    private final ConfigManager config;

    public WealthTaxTask(AltitudeBankPlugin plugin) {
        this.plugin      = plugin;
        this.bankManager = plugin.getBankManager();
        this.config      = plugin.getConfigManager();
    }

    @Override
    public void run() {
        if (!config.isWealthTaxEnabled()) return;

        long now            = System.currentTimeMillis();
        long intervalMs     = (long) (config.getWealthTaxIntervalDays() * 24 * 60 * 60 * 1000L);
        double taxPercent   = config.getWealthTaxPercent();
        double minBalance   = config.getWealthTaxMinBalance();
        int dp              = config.getDecimalPlaces();

        for (Map.Entry<UUID, BankAccount> entry : bankManager.getAllAccounts().entrySet()) {
            BankAccount account = entry.getValue();
            UUID uuid           = entry.getKey();

            // Check if interval has elapsed for this account
            if (now - account.getLastWealthTaxTime() < intervalMs) continue;

            double balance = account.getBalance();
            if (balance < minBalance) {
                // Still update the timestamp so they don't get a burst of taxes later
                account.setLastWealthTaxTime(now);
                continue;
            }

            // Bypass permission check (player may be offline)
            Player onlinePlayer = plugin.getServer().getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.hasPermission("altitudebank.bypass.wealthtax")) {
                account.setLastWealthTaxTime(now);
                continue;
            }

            double taxAmount = balance * (taxPercent / 100.0);
            account.withdraw(taxAmount);
            account.setLastWealthTaxTime(now);

            // Notify online players immediately
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                String msg = MessageUtil.replace(
                        config.getPrefixedMessage("wealth-tax-applied"),
                        "percent", String.valueOf(taxPercent),
                        "amount",  MessageUtil.formatAmount(taxAmount, dp));
                MessageUtil.send(onlinePlayer, msg);

                if (config.isWealthTaxSoundEnabled()) {
                    SoundUtil.playRaw(onlinePlayer, config.getWealthTaxSound(),
                            1.0f, 1.0f, plugin.getLogger());
                }
            } else {
                // Queue join notification for offline player
                if (config.isWealthTaxNotifyOnJoin()) {
                    // Accumulate if multiple cycles pass while offline
                    double existing = account.getPendingWealthTaxNotification();
                    account.setPendingWealthTaxNotification(existing + taxAmount);
                }
            }
        }
    }
}
