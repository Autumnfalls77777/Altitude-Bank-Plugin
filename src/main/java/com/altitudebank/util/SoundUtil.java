package com.altitudebank.util;

import com.altitudebank.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * SoundUtil — safely plays sounds using the config sound keys.
 * Silently ignores invalid sound names rather than crashing.
 */
public final class SoundUtil {

    private SoundUtil() {}

    /**
     * Plays a sound defined by the config sound key (e.g. "deposit-success").
     * Catches invalid sound names and logs a warning.
     *
     * @param player    target player
     * @param config    ConfigManager instance
     * @param key       sounds section key (e.g. "deposit-success")
     * @param logger    plugin logger for warnings
     */
    public static void play(Player player, ConfigManager config, String key, Logger logger) {
        if (!config.isSoundEnabled(key)) return;
        String soundName = config.getSound(key);
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(java.util.Locale.ROOT));
            player.playSound(player.getLocation(), sound,
                    config.getSoundVolume(key), config.getSoundPitch(key));
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid sound name '" + soundName + "' for key '" + key + "'.");
        }
    }

    /**
     * Plays a raw Sound enum value directly.
     */
    public static void playRaw(Player player, String soundName, float volume, float pitch, Logger logger) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(java.util.Locale.ROOT));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid sound name: " + soundName);
        }
    }
}
