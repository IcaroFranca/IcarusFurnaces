package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Optional;

/**
 * Vanilla's {@code RecipeChoice} can only match an ingredient by Material
 * (or a short list of them) — it has no way to require "this exact custom
 * item" for a kit carrying its own PDC tier tag. So every tier's recipe but
 * {@link FurnaceTier#COPPER} declares its "previous tier's kit" slot broadly
 * (see {@code FurnaceKitRegistry}, any player head or the previous tier's
 * fallback material), and this listener does the real check itself — by the
 * item's {@code UPGRADE_KIT_TIER} PDC tag, not by comparing items — every
 * time the crafting grid changes, clearing the result unless what's actually
 * sitting there is the correct previous tier's kit.
 */
public final class FurnaceKitValidationListener implements Listener {

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof Keyed keyed)) {
            return;
        }
        NamespacedKey key = keyed.getKey();
        Optional<FurnaceTier> tier = FurnaceTier.fromUpgradeKitKey(key.getKey());
        if (tier.isEmpty()) {
            return; // not one of our kit recipes
        }
        Optional<FurnaceTier> requiredPrevious = tier.get().previous();
        if (requiredPrevious.isEmpty()) {
            return; // Copper: the grid already requires a literal Material.FURNACE, nothing PDC-based to check
        }

        CraftingInventory inventory = event.getInventory();
        boolean hasCorrectPreviousKit = false;
        for (ItemStack item : inventory.getMatrix()) {
            if (FurnaceKitRegistry.targetTierOf(item).filter(found -> found == requiredPrevious.get()).isPresent()) {
                hasCorrectPreviousKit = true;
                break;
            }
        }
        if (!hasCorrectPreviousKit) {
            inventory.setResult(null);
        }
    }
}
