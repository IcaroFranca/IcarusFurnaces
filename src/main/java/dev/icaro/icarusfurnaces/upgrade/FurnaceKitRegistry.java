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
import org.bukkit.inventory.RecipeChoice;
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
 * upgrades a furnace TO — it is not itself placeable, and applying it is
 * handled by {@code FurnaceInteractListener}.
 *
 * <p>Every tier but {@link FurnaceTier#COPPER} chains off the previous
 * tier's kit sitting in the center of the crafting grid, exactly like
 * IcarusChests' Stack upgrade tiers (see that project's {@code
 * UpgradeRegistry}): vanilla's {@code RecipeChoice} can only match an
 * ingredient by Material, never by "this exact custom item", so that slot is
 * declared broadly (any player head, or the previous tier's own fallback
 * material) and {@link FurnaceKitValidationListener} does the real,
 * PDC-based exactness check as the crafting grid changes.
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
        if (spec.kitChar() != null) {
            recipe.setIngredient(spec.kitChar(), previousKitChoice(tier));
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
        String target = tier.previous().map(previous -> "Fornalha de " + previous.displayName()).orElse("fornalha comum");
        return List.of(
                Component.text("Shift + botao direito numa", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(target + " para evoluir.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        );
    }

    /**
     * The 3x3 grid of display items for {@code tier}'s recipe — real
     * materials plus a sample of the previous tier's kit where relevant —
     * built from the exact same {@link #recipeOf(FurnaceTier)} that {@link
     * #registerRecipe(FurnaceTier)} feeds into the real recipe, so the
     * recipe book (see {@code FurnaceRecipeBookRegistry}) can never drift
     * from it.
     */
    public Map<Integer, ItemStack> recipeBookGrid(FurnaceTier tier) {
        KitRecipe spec = recipeOf(tier);
        String[] shape = spec.shape();
        Map<Integer, ItemStack> grid = new LinkedHashMap<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char symbol = shape[row].charAt(col);
                ItemStack display = displayItemFor(tier, spec, symbol);
                if (display != null) {
                    grid.put(row * 3 + col, display);
                }
            }
        }
        return grid;
    }

    private ItemStack displayItemFor(FurnaceTier tier, KitRecipe spec, char symbol) {
        if (symbol == ' ') {
            return null;
        }
        // spec.kitChar() is null for Copper (no previous-kit slot at all) — the null check has to
        // come first, or comparing a primitive char to a null Character throws on unboxing.
        if (spec.kitChar() != null && symbol == spec.kitChar()) {
            return tier.previous().map(this::createKit).orElse(new ItemStack(Material.FURNACE));
        }
        Material material = spec.materials().get(symbol);
        return material == null ? null : new ItemStack(material);
    }

    private RecipeChoice previousKitChoice(FurnaceTier tier) {
        FurnaceTier previous = tier.previous()
                .orElseThrow(() -> new IllegalStateException(tier + " has no previous tier to chain its kit from"));
        // Accepts either shape createKit(previous) might currently have — a custom head if
        // configured, or its plain fallback material otherwise — so a match is possible regardless.
        return new RecipeChoice.MaterialChoice(Material.PLAYER_HEAD, representativeMaterial(previous));
    }

    /**
     * Each tier's recipe: {@link FurnaceTier#COPPER} is the only one built
     * from a literal {@link Material#FURNACE} (char {@code 'F'}) instead of
     * a previous kit; every other tier's {@code kitChar} names which symbol
     * in its shape stands for the previous tier's kit item (validated by
     * {@link FurnaceKitValidationListener}, never a plain {@code Material}).
     * {@link FurnaceTier#DIAMOND} and {@link FurnaceTier#NETHERITE} are the
     * two tiers the user asked for by name rather than the general "kit +
     * 8 of the tier's material" ring shape.
     */
    private KitRecipe recipeOf(FurnaceTier tier) {
        return switch (tier) {
            case COPPER -> new KitRecipe(
                    new String[]{"MMM", "MFM", "MMM"},
                    Map.of('M', Material.COPPER_INGOT, 'F', Material.FURNACE),
                    null);
            case IRON -> ringRecipe(Material.IRON_INGOT);
            case GOLD -> ringRecipe(Material.GOLD_INGOT);
            case DIAMOND -> new KitRecipe(
                    new String[]{"DGD", "GPG", "DGD"},
                    Map.of('D', Material.DIAMOND, 'G', Material.GLASS),
                    'P');
            case EMERALD -> ringRecipe(Material.EMERALD);
            case OBSIDIAN -> ringRecipe(Material.OBSIDIAN);
            case NETHERITE -> new KitRecipe(
                    new String[]{"   ", "NPN", "   "},
                    Map.of('N', Material.NETHERITE_INGOT),
                    'P');
        };
    }

    /** The general rule: the previous tier's kit in the center, surrounded by 8 of {@code material}. */
    private KitRecipe ringRecipe(Material material) {
        return new KitRecipe(new String[]{"MMM", "MPM", "MMM"}, Map.of('M', material), 'P');
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

    /** One tier's crafting shape plus its plain-material ingredients; {@code kitChar} names the previous-kit slot, if any. */
    private record KitRecipe(String[] shape, Map<Character, Material> materials, Character kitChar) {
    }
}
