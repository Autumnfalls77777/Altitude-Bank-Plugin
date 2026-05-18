package com.altitudebank.listener;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.manager.VaultManager;
import com.altitudebank.util.MessageUtil;
import com.altitudebank.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

/**
 * DeathLossListener — removes a configurable percentage of a player's WALLET
 * (not bank balance) when they die.
 *
 * Bank balance is deliberately untouched: players keep their savings safe.
 */
public class DeathLossListener implements Listener {

    private final AltitudeBankPlugin plugin;
    private final ConfigManager config;
    private final VaultManager vault;

    public DeathLossListener(AltitudeBankPlugin plugin) {
        this.plugin  = plugin;
        this.config  = plugin.getConfigManager();
        this.vault   = plugin.getVaultManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.isDeathLossEnabled()) return;

        Player player = event.getPlayer();

        // Permission bypass
        if (player.hasPermission("altitudebank.bypass.deathloss")) {
            return;
        }

        // World check
        List<String> enabledWorlds = config.getDeathLossWorlds();
        if (!enabledWorlds.isEmpty()
                && !enabledWorlds.contains(player.getWorld().getName())) {
            return;
        }

        double walletBalance = vault.getBalance(player);

        // Minimum wallet threshold
        if (walletBalance < config.getDeathLossMinWallet()) return;

        // Calculate loss
        double lossPercent = config.getDeathLossPercent();
        double loss = walletBalance * (lossPercent / 100.0);

        // Clamp to configured min/max
        double minLoss = config.getDeathLossMin();
        double maxLoss = config.getDeathLossMax();

        if (minLoss > 0) loss = Math.max(loss, minLoss);
        if (maxLoss > 0) loss = Math.min(loss, maxLoss);

        // Sanity: never remove more than wallet holds
        loss = Math.min(loss, walletBalance);

        if (loss <= 0) return;

        // Deduct from wallet via Vault
        boolean success = vault.withdrawWallet(player, loss);
        if (!success) return;

        int dp = config.getDecimalPlaces();
        String msg = MessageUtil.replace(
                config.getPrefixedMessage("death-loss-message"),
                "amount",  MessageUtil.formatAmount(loss, dp),
                "percent", String.valueOf(lossPercent));

        MessageUtil.send(player, msg);

        // Play sound (death sound runs after a tick — schedule slightly delayed)
        if (config.isDeathLossSoundEnabled()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    SoundUtil.playRaw(player, config.getDeathLossSound(),
                            1.0f, 1.0f, plugin.getLogger()), 5L);
        }
    }
}
