package dev.icaro.icarusfurnaces.gui;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds one {@link RecipeBookEntry} per craftable IcarusFurnaces item —
 * every tier's upgrade kit — for the in-game recipe book GUI (see {@code
 * RecipeBookIndexGui}/{@code RecipeBookDetailGui}/{@code RecipeBookListener}).
 * Entries are rebuilt fresh on every open rather than cached, so the shown
 * icons always match whatever's actually registered (custom heads from
 * {@code config.yml} included) even right after a {@code /icarusfurnaces reload}.
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
}
