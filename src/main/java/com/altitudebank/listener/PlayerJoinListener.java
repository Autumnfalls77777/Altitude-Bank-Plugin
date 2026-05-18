package com.altitudebank.listener;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.manager.BankManager;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.model.BankAccount;
import com.altitudebank.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * PlayerJoinListener — fires on player join to:
 *   1. Ensure the player has a bank account loaded.
 *   2. Notify the player if wealth tax was applied while they were offline.
 */
public class PlayerJoinListener implements Listener {

    private final AltitudeBankPlugin plugin;
    private final ConfigManager config;
    private final BankManager bankManager;

    public PlayerJoinListener(AltitudeBankPlugin plugin) {
        this.plugin      = plugin;
        this.config      = plugin.getConfigManager();
        this.bankManager = plugin.getBankManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Ensure account exists (creates one if new player)
        BankAccount account = bankManager.getOrCreate(player.getUniqueId());

        // Offline wealth tax notification
        if (config.isWealthTaxEnabled() && config.isWealthTaxNotifyOnJoin()) {
            double pending = account.getPendingWealthTaxNotification();
            if (pending > 0) {
                // Delay by 2 ticks so join message clears first
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    String msg = MessageUtil.replace(
                            config.getPrefixedMessage("wealth-tax-join-notify"),
                            "percent", String.valueOf(config.getWealthTaxPercent()),
                            "amount",  MessageUtil.formatAmount(pending, config.getDecimalPlaces()));
                    MessageUtil.send(player, msg);
                    // Clear the notification
                    account.setPendingWealthTaxNotification(0);
                }, 2L);
            }
        }
    }
}
