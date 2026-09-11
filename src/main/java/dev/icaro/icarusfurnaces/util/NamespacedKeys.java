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

    /** Marker tag on the Recipe Book item's PDC — present (value irrelevant) means "this is the recipe book". */
    public static NamespacedKey RECIPE_BOOK;

    /** Tag on a recipe book navigation button's PDC: {@code "prev"} or {@code "next"}. */
    public static NamespacedKey RECIPE_NAV;

    public static void init(JavaPlugin plugin) {
        FURNACE_TIER = new NamespacedKey(plugin, "furnace_tier");
        UPGRADE_KIT_TIER = new NamespacedKey(plugin, "upgrade_kit_tier");
        RECIPE_BOOK = new NamespacedKey(plugin, "recipe_book");
        RECIPE_NAV = new NamespacedKey(plugin, "recipe_nav");
    }
}
