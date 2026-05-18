package com.altitudebank.manager;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.util.MessageUtil;
import com.altitudebank.util.SoundUtil;
import org.bukkit.entity.Player;

/**
 * BankService — applies all business rules when executing bank transactions.
 *
 * Keeps command handlers thin: they just parse input, then delegate here.
 */
public class BankService {

    public enum Result { SUCCESS, INSUFFICIENT_FUNDS, BELOW_MIN, ABOVE_MAX, BALANCE_CAP, VAULT_ERROR }

    public record TransactionResult(Result result, double amount, double netAmount,
                                    double taxAmount, double newBalance, String message) {}

    private final AltitudeBankPlugin plugin;
    private final ConfigManager config;
    private final BankManager bankManager;
    private final VaultManager vault;

    public BankService(AltitudeBankPlugin plugin) {
        this.plugin = plugin;
        this.config  = plugin.getConfigManager();
        this.bankManager = plugin.getBankManager();
        this.vault   = plugin.getVaultManager();
    }

    // ── Deposit ───────────────────────────────────────────────

    /**
     * Deposits {@code amount} from the player's Vault wallet into their bank.
     *
     * @param player target
     * @param amount requested deposit amount; pass {@code Double.MAX_VALUE} for "all"
     */
    public TransactionResult deposit(Player player, double amount) {
        int dp = config.getDecimalPlaces();
        double walletBalance = vault.getBalance(player);

        // Clamp "all" to wallet
        double toDeposit = Math.min(amount, walletBalance);

        if (toDeposit <= 0 || walletBalance <= 0) {
            return fail(Result.INSUFFICIENT_FUNDS,
                    config.getPrefixedMessage("deposit-all-nothing"));
        }

        // Minimum check
        double min = config.getMinDeposit();
        if (toDeposit < min) {
            String msg = MessageUtil.replace(
                    config.getPrefixedMessage("deposit-min-error"),
                    "min", MessageUtil.formatAmount(min, dp));
            return fail(Result.BELOW_MIN, msg);
        }

        // Maximum single-deposit check
        double maxSingle = config.getMaxSingleDeposit();
        if (maxSingle > 0 && toDeposit > maxSingle) {
            String msg = MessageUtil.replace(
                    config.getPrefixedMessage("deposit-max-error"),
                    "max", MessageUtil.formatAmount(maxSingle, dp));
            return fail(Result.ABOVE_MAX, msg);
        }

        // Maximum bank balance cap
        double maxBal = config.getMaxBalance();
        double currentBank = bankManager.getBalance(player.getUniqueId());
        if (maxBal > 0 && currentBank + toDeposit > maxBal) {
            // Clamp to the remaining space
            double remaining = maxBal - currentBank;
            if (remaining <= 0) {
                String msg = MessageUtil.replace(
                        config.getPrefixedMessage("deposit-max-balance-error"),
                        "max", MessageUtil.formatAmount(maxBal, dp));
                return fail(Result.BALANCE_CAP, msg);
            }
            toDeposit = remaining;
        }

        // Vault withdraw
        if (!vault.withdrawWallet(player, toDeposit)) {
            return fail(Result.VAULT_ERROR, config.getPrefixedMessage("vault-not-found"));
        }

        double newBalance = bankManager.deposit(player.getUniqueId(), toDeposit);

        String msg = MessageUtil.replace(
                config.getPrefixedMessage("deposit-success"),
                "amount",  MessageUtil.formatAmount(toDeposit, dp),
                "balance", MessageUtil.formatAmount(newBalance, dp));

        SoundUtil.play(player, config, "deposit-success", plugin.getLogger());

        return new TransactionResult(Result.SUCCESS, toDeposit, toDeposit, 0, newBalance, msg);
    }

    // ── Withdraw ──────────────────────────────────────────────

    /**
     * Withdraws {@code amount} from the player's bank into their Vault wallet.
     * Applies withdrawal tax unless the player has the bypass permission.
     *
     * @param player target
     * @param amount requested amount; pass {@code Double.MAX_VALUE} for "all"
     */
    public TransactionResult withdraw(Player player, double amount) {
        int dp = config.getDecimalPlaces();
        double bankBalance = bankManager.getBalance(player.getUniqueId());

        if (bankBalance <= 0) {
            return fail(Result.INSUFFICIENT_FUNDS,
                    config.getPrefixedMessage("withdraw-all-nothing"));
        }

        double toWithdraw = Math.min(amount, bankBalance);

        // Minimum check
        double min = config.getMinWithdraw();
        if (toWithdraw < min) {
            String msg = MessageUtil.replace(
                    config.getPrefixedMessage("withdraw-min-error"),
                    "min", MessageUtil.formatAmount(min, dp));
            return fail(Result.BELOW_MIN, msg);
        }

        // Maximum single-withdrawal check
        double maxSingle = config.getMaxSingleWithdraw();
        if (maxSingle > 0 && toWithdraw > maxSingle) {
            String msg = MessageUtil.replace(
                    config.getPrefixedMessage("withdraw-max-error"),
                    "max", MessageUtil.formatAmount(maxSingle, dp));
            return fail(Result.ABOVE_MAX, msg);
        }

        // Insufficient funds check
        if (toWithdraw > bankBalance) {
            String msg = MessageUtil.replace(
                    config.getPrefixedMessage("withdraw-insufficient"),
                    "amount", MessageUtil.formatAmount(toWithdraw, dp));
            return fail(Result.INSUFFICIENT_FUNDS, msg);
        }

        // Calculate tax
        boolean taxEnabled = config.isWithdrawalTaxEnabled();
        boolean taxBypassed = player.hasPermission("altitudebank.bypass.withdrawtax");
        double taxAmount = 0;
        double netAmount = toWithdraw;

        if (taxEnabled && !taxBypassed && toWithdraw >= config.getMinTaxableWithdrawal()) {
            taxAmount = toWithdraw * (config.getWithdrawalTaxPercent() / 100.0);
            netAmount = toWithdraw - taxAmount;
        }

        // Deduct from bank
        bankManager.withdraw(player.getUniqueId(), toWithdraw);
        double newBalance = bankManager.getBalance(player.getUniqueId());

        // Give net amount to wallet
        vault.depositWallet(player, netAmount);

        String msg = MessageUtil.replace(
                config.getPrefixedMessage("withdraw-success"),
                "amount",  MessageUtil.formatAmount(toWithdraw, dp),
                "balance", MessageUtil.formatAmount(newBalance, dp));

        // Append tax notice if tax was applied
        if (taxAmount > 0) {
            String taxNotice = MessageUtil.replace(
                    config.getPrefixedMessage("withdraw-tax-notice"),
                    "tax_percent",  String.valueOf(config.getWithdrawalTaxPercent()),
                    "net",          MessageUtil.formatAmount(netAmount, dp),
                    "tax_amount",   MessageUtil.formatAmount(taxAmount, dp));
            msg = msg + "\n" + taxNotice;
        }

        SoundUtil.play(player, config, "withdraw-success", plugin.getLogger());

        return new TransactionResult(Result.SUCCESS, toWithdraw, netAmount, taxAmount, newBalance, msg);
    }

    // ── Helper ────────────────────────────────────────────────

    private TransactionResult fail(Result result, String message) {
        return new TransactionResult(result, 0, 0, 0, 0, message);
    }
}
