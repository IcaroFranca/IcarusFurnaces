package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Stops an upgrade kit from ever being placed as a block. Most tiers'
 * fallback icon is a safe, non-placeable item (an ingot or a gem), but
 * Obsidian's is deliberately a real, placeable {@code Material.OBSIDIAN}
 * block for an obvious visual match — placing it would silently turn a
 * valuable kit into a plain world block, losing its {@code
 * UPGRADE_KIT_TIER} PDC tag for good. Cancelling by PDC tag rather than by
 * material also means any future tier is covered automatically, whatever
 * icon it ends up using.
 */
public final class FurnaceKitProtectionListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (FurnaceKitRegistry.targetTierOf(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }
}
