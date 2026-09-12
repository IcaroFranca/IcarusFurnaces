package dev.icaro.icarusfurnaces.upgrade;

import dev.icaro.icarusfurnaces.config.ConfigManager;
import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.util.CustomHeads;
import dev.icaro.icarusfurnaces.util.NamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Builds the consumable "upgrade kit" item for each {@link FurnaceTier} and
 * registers its crafting recipe. A kit is tagged via PDC with the tier it
 * upgrades a furnace TO — it is not itself placeable, and applying it (via
 * shift + right-click on a placed furnace at the right current tier) is
 * handled by {@code FurnaceInteractListener}.
 *
 * <p>Every tier's kit is crafted the same simple way: a plain {@link
 * Material#FURNACE} in the center of the grid, surrounded by that tier's own
 * material — no tier's recipe requires already owning a previous tier's kit,
 * unlike an earlier design of this class. The sequential-progression gate
 * lives entirely at application time ({@code FurnaceInteractListener}
 * checking the placed furnace's current tier), not at crafting time — the
 * exact same split IcarusChests' own {@code UpgradeKitRegistry} uses for
 * chest tier kits. That also means every ingredient here is a plain,
 * unambiguous {@code Material}: no custom-item PDC exactness check is needed
 * (contrast with IcarusChests' Stack upgrade tiers, which do chain off a
 * previous custom item and need exactly that kind of check).
 *
 * <p>The icon is a custom-textured player head when the admin configured one
 * for that tier ({@code upgrade-kit-heads} in {@code config.yml}), falling
 * back to a plain icon of the tier's own representative material otherwise
 * (see {@link #representativeMaterial}). The Obsidian tier's fallback icon is
 * a placeable block ({@link Material#OBSIDIAN} itself, for an obvious visual
 * match) — {@code FurnaceKitProtectionListener} is what actually keeps any
 * kit item, of any material, from being placed and silently losing its PDC
 * identity, so no tier needs to avoid a placeable material for its icon.
 */
public final class FurnaceKitRegistry {

    private final Plugin plugin;
    private final ConfigManager configManager;

    public FurnaceKitRegistry(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Registers a shaped crafting recipe for every tier. One tier's problem
     * (e.g. a malformed head texture) is logged and skipped rather than
     * aborting the rest — this runs during {@code onEnable}, and an
     * uncaught exception here would otherwise stop the plugin from ever
     * registering its commands/listeners at all.
     */
    public void registerRecipes() {
        for (FurnaceTier tier : FurnaceTier.values()) {
            try {
                registerRecipe(tier);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Falha ao registrar a receita do kit de upgrade " + tier, e);
            }
        }
    }

    private void registerRecipe(FurnaceTier tier) {
        NamespacedKey key = new NamespacedKey(plugin, tier.upgradeKitKey());
        KitRecipe spec = recipeOf(tier);
        ShapedRecipe recipe = new ShapedRecipe(key, createKit(tier));
        recipe.shape(spec.shape());
        for (Map.Entry<Character, Material> entry : spec.materials().entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        plugin.getServer().addRecipe(recipe);
    }

    /** Builds a fresh kit item for {@code tier}. Does not register a recipe. */
    public ItemStack createKit(FurnaceTier tier) {
        Optional<String> headTexture = configManager.upgradeKitHeadTexture(tier);
        ItemStack item = headTexture.isPresent() ? CustomHeads.createHead(headTexture.get()) : new ItemStack(representativeMaterial(tier));

        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Kit de Upgrade: " + tier.displayName(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(kitLore(tier));
        meta.getPersistentDataContainer().set(NamespacedKeys.UPGRADE_KIT_TIER, PersistentDataType.INTEGER, tier.ordinal());
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> kitLore(FurnaceTier tier) {
        // Describes where to APPLY the kit (shift + right-click), not how to craft it — the
        // sequential-tier gate below is unrelated to this class's crafting recipes, see the class
        // javadoc.
        String target = tier.previous().map(previous -> "Fornalha de " + previous.displayName()).orElse("fornalha comum");
        return List.of(
                Component.text("Shift + botao direito numa", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(target + " para evoluir.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        );
    }

    /**
     * The 3x3 grid of display items for {@code tier}'s recipe, built from the exact same {@link
     * #recipeOf(FurnaceTier)} that {@link #registerRecipe(FurnaceTier)} feeds into the real recipe,
     * so the recipe book (see {@code FurnaceRecipeBookRegistry}) can never drift from it.
     */
    public Map<Integer, ItemStack> recipeBookGrid(FurnaceTier tier) {
        KitRecipe spec = recipeOf(tier);
        String[] shape = spec.shape();
        Map<Integer, ItemStack> grid = new LinkedHashMap<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char symbol = shape[row].charAt(col);
                Material material = spec.materials().get(symbol);
                if (material != null) {
                    grid.put(row * 3 + col, new ItemStack(material));
                }
            }
        }
        return grid;
    }

    /**
     * Each tier's recipe: a plain {@link Material#FURNACE} (char {@code 'F'}) in the center,
     * surrounded by that tier's own material. {@link FurnaceTier#DIAMOND} and {@link
     * FurnaceTier#NETHERITE} are the two tiers the user asked for by name rather than the general
     * "furnace + 8 of the tier's material" ring shape.
     */
    private KitRecipe recipeOf(FurnaceTier tier) {
        return switch (tier) {
            case COPPER -> ringRecipe(Material.COPPER_INGOT);
            case IRON -> ringRecipe(Material.IRON_INGOT);
            case GOLD -> ringRecipe(Material.GOLD_INGOT);
            case DIAMOND -> new KitRecipe(
                    new String[]{"DGD", "GFG", "DGD"},
                    Map.of('D', Material.DIAMOND, 'G', Material.GLASS, 'F', Material.FURNACE));
            case EMERALD -> ringRecipe(Material.EMERALD);
            case OBSIDIAN -> ringRecipe(Material.OBSIDIAN);
            case NETHERITE -> new KitRecipe(
                    new String[]{"   ", "NFN", "   "},
                    Map.of('N', Material.NETHERITE_INGOT, 'F', Material.FURNACE));
        };
    }

    /** The general rule: a plain furnace in the center, surrounded by 8 of {@code material}. */
    private KitRecipe ringRecipe(Material material) {
        return new KitRecipe(new String[]{"MMM", "MFM", "MMM"}, Map.of('M', material, 'F', Material.FURNACE));
    }

    /** The plain-icon material representing this tier when no custom head is configured — never a placeable block. */
    private static Material representativeMaterial(FurnaceTier tier) {
        return switch (tier) {
            case COPPER -> Material.COPPER_INGOT;
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case DIAMOND -> Material.DIAMOND;
            case EMERALD -> Material.EMERALD;
            case OBSIDIAN -> Material.OBSIDIAN; // placeable — safe only because FurnaceKitProtectionListener blocks placing any kit item
            case NETHERITE -> Material.NETHERITE_INGOT;
        };
    }

    /** The tier an item upgrades a furnace TO, if it's a valid kit at all. */
    public static Optional<FurnaceTier> targetTierOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return Optional.empty();
        }
        Integer ordinal = item.getItemMeta().getPersistentDataContainer()
                .get(NamespacedKeys.UPGRADE_KIT_TIER, PersistentDataType.INTEGER);
        return ordinal == null ? Optional.empty() : FurnaceTier.byOrdinal(ordinal);
    }

    /** One tier's crafting shape plus its plain-material ingredients (always includes a {@code 'F'} → {@link Material#FURNACE} entry). */
    private record KitRecipe(String[] shape, Map<Character, Material> materials) {
    }
}
