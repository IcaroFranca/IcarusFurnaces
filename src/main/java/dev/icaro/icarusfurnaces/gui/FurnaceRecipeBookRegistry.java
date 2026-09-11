package dev.icaro.icarusfurnaces.gui;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import dev.icaro.icarusfurnaces.util.NamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds one {@link RecipeBookEntry} per craftable IcarusFurnaces item —
 * every tier's upgrade kit — for the in-game recipe book GUI (see {@code
 * RecipeBookGui}/{@code RecipeBookListener}). Entries are rebuilt fresh on
 * every open rather than cached, so the shown icons always match whatever's
 * actually registered (custom heads from {@code config.yml} included) even
 * right after a {@code /icarusfurnaces reload}.
 *
 * <p><b>Convention: every new craftable item this plugin ever gets needs an
 * entry added here too</b> — this is the only place a player can see how to
 * make something without already knowing the recipe by heart.</p>
 */
public final class FurnaceRecipeBookRegistry {

    private final FurnaceKitRegistry furnaceKitRegistry;

    public FurnaceRecipeBookRegistry(FurnaceKitRegistry furnaceKitRegistry) {
        this.furnaceKitRegistry = furnaceKitRegistry;
    }

    /** Every known recipe: one entry per furnace tier's upgrade kit, worst to best. */
    public List<RecipeBookEntry> buildAll() {
        List<RecipeBookEntry> entries = new ArrayList<>();
        for (FurnaceTier tier : FurnaceTier.values()) {
            entries.add(kitEntry(tier));
        }
        return entries;
    }

    private RecipeBookEntry kitEntry(FurnaceTier tier) {
        Component title = Component.text("Kit de Upgrade: " + tier.displayName(), NamedTextColor.LIGHT_PURPLE);
        return new RecipeBookEntry(title, furnaceKitRegistry.recipeBookGrid(tier), furnaceKitRegistry.createKit(tier));
    }

    /** The physical item players right-click to open the recipe book GUI (see {@code RecipeBookListener}). */
    public static ItemStack createBookItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Livro de Receitas", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Clique com o botao direito", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("para ver todas as receitas.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(NamespacedKeys.RECIPE_BOOK, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** Whether {@code item} is the recipe book item (see {@link #createBookItem()}). */
    public static boolean isBookItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(NamespacedKeys.RECIPE_BOOK, PersistentDataType.BYTE);
    }
}
