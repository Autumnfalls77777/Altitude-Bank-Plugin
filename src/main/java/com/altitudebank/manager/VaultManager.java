package com.altitudebank.manager;

import com.altitudebank.AltitudeBankPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * VaultManager — wraps the Vault Economy service.
 * Provides safe deposit/withdraw/balance helpers with null-checks.
 */
public class VaultManager {

    private final AltitudeBankPlugin plugin;
    private Economy economy;

    public VaultManager(AltitudeBankPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No Economy provider found (is EssentialsX installed?).");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Economy hooked: " + economy.getName());
    }

    /** @return true if an economy provider was found */
    public boolean isReady() {
        return economy != null;
    }

    /** Returns the player's current wallet balance. */
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    /**
     * Deposits {@code amount} into the player's wallet.
     * @return true on success
     */
    public boolean depositWallet(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Withdraws {@code amount} from the player's wallet.
     * @return true on success
     */
    public boolean withdrawWallet(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** @return true if player has at least {@code amount} in their wallet */
    public boolean hasWallet(Player player, double amount) {
        return economy.has(player, amount);
    }

    /** @return Vault currency name (singular) */
    public String getCurrencyName() {
        try { return economy.currencyNameSingular(); } catch (Exception e) { return ""; }
    }

    /** @return Vault currency name (plural) */
    public String getCurrencyNamePlural() {
        try { return economy.currencyNamePlural(); } catch (Exception e) { return ""; }
    }

    /** Raw Economy accessor for advanced use */
    public Economy getEconomy() {
        return economy;
    }
}
