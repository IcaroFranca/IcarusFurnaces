package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.tier.FurnaceTierService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;

/**
 * The actual speed effect: overrides how long a single smelt takes on a
 * tagged furnace. Only ever fires anything for {@link Material#FURNACE}
 * blocks, since {@link FurnaceTierService#tierOf} reads a PDC tag this
 * plugin only ever writes onto that block type (see {@code
 * FurnaceInteractListener}) — a Blast Furnace or Smoker is never touched,
 * by construction, matching the deliberate MVP scope (plain Furnace only).
 */
public final class FurnaceCookSpeedListener implements Listener {

    @EventHandler
    public void onStartSmelt(FurnaceStartSmeltEvent event) {
        if (event.getBlock().getType() != Material.FURNACE) {
            return;
        }
        FurnaceTierService.tierOf(event.getBlock()).ifPresent(tier -> event.setTotalCookTime(tier.cookTicks()));
    }
}
