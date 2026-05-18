package com.altitudebank.listener;

import com.altitudebank.AltitudeBankPlugin;
import com.altitudebank.manager.BankService;
import com.altitudebank.manager.ChatInputManager;
import com.altitudebank.manager.ConfigManager;
import com.altitudebank.model.ChatInputSession;
import com.altitudebank.util.MessageUtil;
import com.altitudebank.util.SoundUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * ChatInputListener — listens for async chat events to process custom amount input.
 *
 * Uses AsyncChatEvent (Paper) which fires asynchronously.
 * We reschedule processing back to the main thread to safely interact with Vault/BankManager.
 */
public class ChatInputListener implements Listener {

    private final AltitudeBankPlugin plugin;
    private final ChatInputManager chatInputManager;
    private final ConfigManager config;
    private final BankService bankService;

    public ChatInputListener(AltitudeBankPlugin plugin) {
        this.plugin           = plugin;
        this.chatInputManager = plugin.getChatInputManager();
        this.config           = plugin.getConfigManager();
        this.bankService      = new BankService(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatInputSession session = chatInputManager.getSession(player.getUniqueId());
        if (session == null) return; // player is not in input mode

        // Cancel the public chat message
        event.setCancelled(true);

        // Extract plain text from the Adventure component
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Handle "cancel" keyword
        if (rawMessage.equalsIgnoreCase("cancel")) {
            chatInputManager.clearSession(player.getUniqueId());
            // Schedule message send on main thread
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.send(player, config.getPrefixedMessage("custom-input-cancel")));
            return;
        }

        // Parse the amount
        double amount;
        try {
            amount = MessageUtil.parseAmount(rawMessage);
        } catch (NumberFormatException e) {
            // Keep session active; re-prompt
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                SoundUtil.play(player, config, "error", plugin.getLogger());
                MessageUtil.send(player, config.getPrefixedMessage("deposit-invalid-amount"));
                // Re-prompt based on session type
                if (session.getType() == ChatInputSession.Type.DEPOSIT) {
                    MessageUtil.send(player, config.getPrefixedMessage("custom-input-prompt-deposit"));
                } else {
                    MessageUtil.send(player, config.getPrefixedMessage("custom-input-prompt-withdraw"));
                }
            });
            return;
        }

        // Clear session before processing
        chatInputManager.clearSession(player.getUniqueId());

        // All bank operations must happen on the main thread
        final double finalAmount = amount;
        final ChatInputSession.Type type = session.getType();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            BankService.TransactionResult result;
            if (type == ChatInputSession.Type.DEPOSIT) {
                result = bankService.deposit(player, finalAmount);
            } else {
                result = bankService.withdraw(player, finalAmount);
            }
            MessageUtil.send(player, result.message());
            if (result.result() != BankService.Result.SUCCESS) {
                SoundUtil.play(player, config, "error", plugin.getLogger());
            }
        });
    }
}
