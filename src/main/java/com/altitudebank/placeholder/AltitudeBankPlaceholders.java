package com.altitudebank.placeholder;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.manager.BankManager;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.util.MessageUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AltitudeBankPlaceholders — registers %altitudebank_*% placeholders with PlaceholderAPI.
 *
 * Available placeholders:
 *   %altitudebank_balance%             → raw balance (e.g. 123456.78)
 *   %altitudebank_formatted_balance%   → formatted balance (e.g. 123,456.78)
 *   %altitudebank_tax%                 → current withdrawal tax percent
 *   %altitudebank_wealth_tax%          → current wealth tax percent
 *   %altitudebank_wealth_tax_interval% → wealth tax interval in days
 */
public class AltitudeBankPlaceholders extends PlaceholderExpansion {

    private final AltitudeBankPlugin plugin;
    private final BankManager bankManager;
    private final ConfigManager config;

    public AltitudeBankPlaceholders(AltitudeBankPlugin plugin) {
        this.plugin      = plugin;
        this.bankManager = plugin.getBankManager();
        this.config      = plugin.getConfigManager();
    }

    @Override
    public @NotNull String getIdentifier() { return "altitudebank"; }

    @Override
    public @NotNull String getAuthor() { return "AltitudeBank"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    // Persist across PlaceholderAPI reloads
    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        double balance = bankManager.getBalance(player.getUniqueId());
        int dp = config.getDecimalPlaces();

        return switch (params.toLowerCase()) {
            case "balance"             -> String.valueOf(balance);
            case "formatted_balance"   -> MessageUtil.formatAmount(balance, dp);
            case "tax"                 -> String.valueOf(config.getWithdrawalTaxPercent());
            case "wealth_tax"          -> String.valueOf(config.getWealthTaxPercent());
            case "wealth_tax_interval" -> String.valueOf(config.getWealthTaxIntervalDays());
            default                    -> null; // PAPI returns "" for null
        };
    }
}
