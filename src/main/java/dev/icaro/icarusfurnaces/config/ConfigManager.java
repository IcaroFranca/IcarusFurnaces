package dev.icaro.icarusfurnaces.config;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Thin wrapper around {@code config.yml}. Deliberately minimal — tick counts
 * stay compile-time constants in {@link FurnaceTier} rather than becoming a
 * fully data-driven registry; the only thing an admin can configure so far is
 * a cosmetic custom-head texture per kit tier.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<FurnaceTier, String> upgradeKitHeadTextures = new EnumMap<>(FurnaceTier.class);

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads (or reloads) {@code config.yml} from disk. */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mergeNewDefaults();

        upgradeKitHeadTextures.clear();
        ConfigurationSection kitHeads = plugin.getConfig().getConfigurationSection("upgrade-kit-heads");
        if (kitHeads != null) {
            for (FurnaceTier tier : FurnaceTier.values()) {
                String texture = kitHeads.getString(tier.name().toLowerCase(), "");
                if (texture != null && !texture.isBlank()) {
                    upgradeKitHeadTextures.put(tier, texture.trim());
                }
            }
        }
    }

    /**
     * Fills in any key the jar's bundled {@code config.yml} defines but the
     * admin's on-disk file doesn't have yet (e.g. a texture added in a newer
     * plugin version) and persists the result — see IcarusChests'
     * {@code ConfigManager} for the identical reasoning.
     */
    private void mergeNewDefaults() {
        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            FileConfiguration config = plugin.getConfig();
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            plugin.saveConfig();
            plugin.reloadConfig();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao mesclar novos valores padrao no config.yml", e);
        }
    }

    /** The configured custom-head Base64 texture for this tier's upgrade kit, if the admin set one. */
    public Optional<String> upgradeKitHeadTexture(FurnaceTier tier) {
        return Optional.ofNullable(upgradeKitHeadTextures.get(tier));
    }
}
