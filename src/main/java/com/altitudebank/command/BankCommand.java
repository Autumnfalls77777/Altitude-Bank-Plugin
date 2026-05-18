package com.altitudebank.command;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.manager.BankService;
import com.altitudebank.manager.ChatInputManager;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.model.ChatInputSession;
import com.altitudebank.util.MessageUtil;
import com.altitudebank.util.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BankCommand — handles all /bank subcommands.
 *
 * Subcommands:
 *   /bank                  → help menu
 *   /bank balance          → show balance
 *   /bank deposit <amt|all>
 *   /bank deposithalf
 *   /bank depositcustom
 *   /bank withdraw <amt|all>
 *   /bank withdrawhalf
 *   /bank withdrawcustom
 */
public class BankCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "balance", "deposit", "deposithalf", "depositcustom",
            "withdraw", "withdrawhalf", "withdrawcustom"
    );

    private final AltitudeBankPlugin plugin;
    private final ConfigManager config;
    private final BankService bankService;
    private final ChatInputManager chatInputManager;

    public BankCommand(AltitudeBankPlugin plugin) {
        this.plugin           = plugin;
        this.config           = plugin.getConfigManager();
        this.bankService      = new BankService(plugin);
        this.chatInputManager = plugin.getChatInputManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("altitudebank.use")) {
            MessageUtil.send(player, config.getPrefixedMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "balance"        -> handleBalance(player);
            case "deposit"        -> handleDeposit(player, args);
            case "deposithalf"    -> handleDepositHalf(player);
            case "depositcustom"  -> handleDepositCustom(player);
            case "withdraw"       -> handleWithdraw(player, args);
            case "withdrawhalf"   -> handleWithdrawHalf(player);
            case "withdrawcustom" -> handleWithdrawCustom(player);
            default               -> MessageUtil.send(player, config.getPrefixedMessage("unknown-subcommand"));
        }

        return true;
    }

    // ── Subcommand handlers ───────────────────────────────────

    private void handleBalance(Player player) {
        int dp = config.getDecimalPlaces();
        double bankBal   = plugin.getBankManager().getBalance(player.getUniqueId());
        double walletBal = plugin.getVaultManager().getBalance(player);

        String balMsg    = MessageUtil.replace(config.getPrefixedMessage("balance-display"),
                "balance", MessageUtil.formatAmount(bankBal, dp));
        String walletMsg = MessageUtil.replace(config.getPrefixedMessage("wallet-display"),
                "wallet", MessageUtil.formatAmount(walletBal, dp));

        MessageUtil.send(player, balMsg);
        MessageUtil.send(player, walletMsg);
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, config.getPrefix() + "&eUsage: /bank deposit <amount|all>");
            return;
        }

        double amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = Double.MAX_VALUE; // BankService clamps to wallet balance
        } else {
            try {
                amount = MessageUtil.parseAmount(args[1]);
            } catch (NumberFormatException e) {
                SoundUtil.play(player, config, "error", plugin.getLogger());
                MessageUtil.send(player, config.getPrefixedMessage("deposit-invalid-amount"));
                return;
            }
        }

        // Wallet check before service call
        if (!plugin.getVaultManager().hasWallet(player, 0.01)) {
            MessageUtil.send(player, config.getPrefixedMessage("deposit-all-nothing"));
            return;
        }

        BankService.TransactionResult result = bankService.deposit(player, amount);
        MessageUtil.send(player, result.message());
        if (result.result() != BankService.Result.SUCCESS) {
            SoundUtil.play(player, config, "error", plugin.getLogger());
        }
    }

    private void handleDepositHalf(Player player) {
        double wallet = plugin.getVaultManager().getBalance(player);
        double half   = wallet / 2.0;

        if (half < config.getMinDeposit()) {
            MessageUtil.send(player, config.getPrefixedMessage("deposit-all-nothing"));
            return;
        }

        BankService.TransactionResult result = bankService.deposit(player, half);
        MessageUtil.send(player, result.message());
        if (result.result() != BankService.Result.SUCCESS) {
            SoundUtil.play(player, config, "error", plugin.getLogger());
        }
    }

    private void handleDepositCustom(Player player) {
        if (chatInputManager.hasSession(player.getUniqueId())) {
            MessageUtil.send(player, config.getPrefixedMessage("custom-input-already-active"));
            return;
        }
        chatInputManager.startSession(player.getUniqueId(), ChatInputSession.Type.DEPOSIT);
        MessageUtil.send(player, config.getPrefixedMessage("custom-input-prompt-deposit"));
        SoundUtil.play(player, config, "custom-input-prompt", plugin.getLogger());
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, config.getPrefix() + "&eUsage: /bank withdraw <amount|all>");
            return;
        }

        double amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = Double.MAX_VALUE; // BankService clamps to bank balance
        } else {
            try {
                amount = MessageUtil.parseAmount(args[1]);
            } catch (NumberFormatException e) {
                SoundUtil.play(player, config, "error", plugin.getLogger());
                MessageUtil.send(player, config.getPrefixedMessage("withdraw-invalid-amount"));
                return;
            }
        }

        BankService.TransactionResult result = bankService.withdraw(player, amount);
        MessageUtil.send(player, result.message());
        if (result.result() != BankService.Result.SUCCESS) {
            SoundUtil.play(player, config, "error", plugin.getLogger());
        }
    }

    private void handleWithdrawHalf(Player player) {
        double bank = plugin.getBankManager().getBalance(player.getUniqueId());
        double half = bank / 2.0;

        if (half < config.getMinWithdraw()) {
            MessageUtil.send(player, config.getPrefixedMessage("withdraw-all-nothing"));
            return;
        }

        BankService.TransactionResult result = bankService.withdraw(player, half);
        MessageUtil.send(player, result.message());
        if (result.result() != BankService.Result.SUCCESS) {
            SoundUtil.play(player, config, "error", plugin.getLogger());
        }
    }

    private void handleWithdrawCustom(Player player) {
        if (chatInputManager.hasSession(player.getUniqueId())) {
            MessageUtil.send(player, config.getPrefixedMessage("custom-input-already-active"));
            return;
        }
        chatInputManager.startSession(player.getUniqueId(), ChatInputSession.Type.WITHDRAW);
        MessageUtil.send(player, config.getPrefixedMessage("custom-input-prompt-withdraw"));
        SoundUtil.play(player, config, "custom-input-prompt", plugin.getLogger());
    }

    // ── Help ──────────────────────────────────────────────────

    private void sendHelp(Player player) {
        MessageUtil.send(player, config.getMessage("help-header"));
        MessageUtil.send(player, config.getMessage("help-balance"));
        MessageUtil.send(player, config.getMessage("help-deposit"));
        MessageUtil.send(player, config.getMessage("help-deposithalf"));
        MessageUtil.send(player, config.getMessage("help-depositcustom"));
        MessageUtil.send(player, config.getMessage("help-withdraw"));
        MessageUtil.send(player, config.getMessage("help-withdrawhalf"));
        MessageUtil.send(player, config.getMessage("help-withdrawcustom"));
        MessageUtil.send(player, config.getMessage("help-footer"));
    }

    // ── Tab completion ─────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("deposit") || sub.equals("withdraw")) {
                return List.of("all", "1000", "10000", "100000");
            }
        }

        return List.of();
    }
}