package dev.icaro.icarusfurnaces;

import dev.icaro.icarusfurnaces.command.IcarusFurnacesCommand;
import dev.icaro.icarusfurnaces.config.ConfigManager;
import dev.icaro.icarusfurnaces.gui.FurnaceRecipeBookRegistry;
import dev.icaro.icarusfurnaces.listener.FurnaceCookSpeedListener;
import dev.icaro.icarusfurnaces.listener.FurnaceInteractListener;
import dev.icaro.icarusfurnaces.listener.FurnaceKitProtectionListener;
import dev.icaro.icarusfurnaces.listener.FurnaceKitValidationListener;
import dev.icaro.icarusfurnaces.listener.FurnaceParticleListener;
import dev.icaro.icarusfurnaces.listener.RecipeBookListener;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import dev.icaro.icarusfurnaces.util.NamespacedKeys;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the IcarusFurnaces plugin — a companion to IcarusChests
 * that speeds up smelting on a plain Furnace through the same kit-upgrade
 * pattern (see the project plan). Unlike IcarusChests, tier is tagged
 * directly on the block's own tile-entity PDC and needs no database at all:
 * a furnace's capacity never changes between tiers, only its cook speed.
 */
public final class IcarusFurnacesPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private FurnaceKitRegistry furnaceKitRegistry;
    private FurnaceRecipeBookRegistry recipeBookRegistry;

    @Override
    public void onEnable() {
        NamespacedKeys.init(this);
        configManager = new ConfigManager(this);
        configManager.load();

        furnaceKitRegistry = new FurnaceKitRegistry(this, configManager);
        recipeBookRegistry = new FurnaceRecipeBookRegistry(furnaceKitRegistry);

        registerCommands();
        registerListeners();
        furnaceKitRegistry.registerRecipes();

        getLogger().info("IcarusFurnaces habilitado (v" + getPluginMeta().getVersion() + ").");
    }

    /** Reloads {@code config.yml} — kit head textures take effect immediately, no restart needed. */
    public void reloadPluginConfig() {
        configManager.load();
    }

    @Override
    public void onDisable() {
        getLogger().info("IcarusFurnaces desabilitado.");
    }

    private void registerCommands() {
        var command = getCommand("icarusfurnaces");
        if (command != null) {
            IcarusFurnacesCommand executor = new IcarusFurnacesCommand(this, furnaceKitRegistry, recipeBookRegistry);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new FurnaceInteractListener(), this);
        pluginManager.registerEvents(new FurnaceCookSpeedListener(), this);
        pluginManager.registerEvents(new FurnaceKitValidationListener(), this);
        pluginManager.registerEvents(new FurnaceKitProtectionListener(), this);
        pluginManager.registerEvents(new FurnaceParticleListener(), this);
        pluginManager.registerEvents(new RecipeBookListener(recipeBookRegistry), this);
    }
}
