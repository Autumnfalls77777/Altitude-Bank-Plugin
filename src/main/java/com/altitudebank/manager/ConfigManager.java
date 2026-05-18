package com.altitudebank.manager;

import com.altitudebank.AltitudeBankPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * ConfigManager — centralised, typed access to config.yml.
 * All other classes pull settings from here rather than accessing config directly.
 */
public class ConfigManager {

    private final AltitudeBankPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(AltitudeBankPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-reads config.yml from disk (supports /bank reload later). */
    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    // ── Economy ───────────────────────────────────────────────

    public double getStartingBalance()      { return cfg.getDouble("economy.starting-balance", 0); }
    public String getCurrencySymbol()       { return cfg.getString("economy.currency-symbol", "$"); }
    public int getDecimalPlaces()           { return cfg.getInt("economy.decimal-places", 2); }
    public double getMinDeposit()           { return cfg.getDouble("economy.min-deposit", 1); }
    public double getMinWithdraw()          { return cfg.getDouble("economy.min-withdraw", 1); }
    public double getMaxSingleDeposit()     { return cfg.getDouble("economy.max-single-deposit", 0); }
    public double getMaxSingleWithdraw()    { return cfg.getDouble("economy.max-single-withdraw", 0); }
    public double getMaxBalance()           { return cfg.getDouble("economy.max-balance", 0); }

    // ── Withdrawal tax ─────────────────────────────────────────

    public boolean isWithdrawalTaxEnabled() { return cfg.getBoolean("withdrawal-tax.enabled", true); }
    public double getWithdrawalTaxPercent() { return cfg.getDouble("withdrawal-tax.tax-percent", 5.0); }
    public double getMinTaxableWithdrawal() { return cfg.getDouble("withdrawal-tax.min-taxable-amount", 100.0); }

    // ── Death loss ─────────────────────────────────────────────

    public boolean isDeathLossEnabled()     { return cfg.getBoolean("death-loss.enabled", true); }
    public double getDeathLossPercent()     { return cfg.getDouble("death-loss.loss-percent", 20.0); }
    public double getDeathLossMinWallet()   { return cfg.getDouble("death-loss.min-wallet-balance", 500.0); }
    public double getDeathLossMin()         { return cfg.getDouble("death-loss.min-loss", 0); }
    public double getDeathLossMax()         { return cfg.getDouble("death-loss.max-loss", 0); }
    public List<String> getDeathLossWorlds() { return cfg.getStringList("death-loss.enabled-worlds"); }
    public boolean isDeathLossSoundEnabled(){ return cfg.getBoolean("death-loss.play-sound", true); }
    public String getDeathLossSound()       { return cfg.getString("death-loss.sound", "ENTITY_WITHER_HURT"); }

    // ── Wealth tax ─────────────────────────────────────────────

    public boolean isWealthTaxEnabled()     { return cfg.getBoolean("wealth-tax.enabled", true); }
    public double getWealthTaxIntervalDays(){ return cfg.getDouble("wealth-tax.interval-days", 3.0); }
    public double getWealthTaxPercent()     { return cfg.getDouble("wealth-tax.tax-percent", 2.0); }
    public double getWealthTaxMinBalance()  { return cfg.getDouble("wealth-tax.min-balance", 10000.0); }
    public boolean isWealthTaxNotifyOnJoin(){ return cfg.getBoolean("wealth-tax.notify-on-join", true); }
    public boolean isWealthTaxSoundEnabled(){ return cfg.getBoolean("wealth-tax.play-sound", true); }
    public String getWealthTaxSound()       { return cfg.getString("wealth-tax.sound", "BLOCK_NOTE_BLOCK_BASS"); }

    // ── Auto-save ──────────────────────────────────────────────

    public int getAutoSaveIntervalSeconds() { return cfg.getInt("autosave.interval-seconds", 300); }

    // ── PlaceholderAPI ─────────────────────────────────────────

    public boolean isPlaceholderApiEnabled(){ return cfg.getBoolean("placeholderapi.enabled", true); }

    // ── Messages ──────────────────────────────────────────────

    public String getPrefix()               { return cfg.getString("messages.prefix", "&8[&6AltitudeBank&8] &r"); }
    public String getMessage(String key)    { return cfg.getString("messages." + key, "&cMissing message: " + key); }

    /** Convenience: get a message with the plugin prefix pre-pended. */
    public String getPrefixedMessage(String key) {
        return getPrefix() + getMessage(key);
    }

    // ── Sounds ────────────────────────────────────────────────

    public boolean isSoundEnabled(String key)  { return cfg.getBoolean("sounds." + key + ".enabled", true); }
    public String getSound(String key)         { return cfg.getString("sounds." + key + ".sound", "UI_BUTTON_CLICK"); }
    public float getSoundVolume(String key)    { return (float) cfg.getDouble("sounds." + key + ".volume", 1.0); }
    public float getSoundPitch(String key)     { return (float) cfg.getDouble("sounds." + key + ".pitch", 1.0); }
}
