package dev.icaro.icarusfurnaces.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central registry of {@link NamespacedKey}s used to tag blocks and items
 * with plugin-owned {@code PersistentDataContainer} data. Must be
 * {@link #init(JavaPlugin)}-ed once during {@code onEnable} before any key
 * is read.
 */
public final class NamespacedKeys {

    private NamespacedKeys() {
    }

    /**
     * Tag on a placed furnace's tile entity PDC identifying its {@code
     * FurnaceTier} ordinal. Absent entirely means "still a plain vanilla
     * furnace, never upgraded" — there is no explicit "tier zero" constant,
     * unlike IcarusChests' {@code ChestTier.NORMAL}, since a furnace has
     * nothing else (capacity, upgrade slots) that would need a placeholder.
     */
    public static NamespacedKey FURNACE_TIER;

    /** Tag on an upgrade kit item's PDC identifying the target {@code FurnaceTier} ordinal. */
    public static NamespacedKey UPGRADE_KIT_TIER;

    /** Tag on the recipe detail screen's "back to index" button's PDC: always {@code "back"} when present. */
    public static NamespacedKey RECIPE_NAV;

    /** Tag on a recipe index screen's icon PDC identifying which built {@code RecipeBookEntry} (by list position) it opens. */
    public static NamespacedKey RECIPE_ENTRY_INDEX;

    public static void init(JavaPlugin plugin) {
        FURNACE_TIER = new NamespacedKey(plugin, "furnace_tier");
        UPGRADE_KIT_TIER = new NamespacedKey(plugin, "upgrade_kit_tier");
        RECIPE_NAV = new NamespacedKey(plugin, "recipe_nav");
        RECIPE_ENTRY_INDEX = new NamespacedKey(plugin, "recipe_entry_index");
    }
}
