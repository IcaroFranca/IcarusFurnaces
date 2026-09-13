package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Stops an upgrade kit from ever being placed as a block. Every kit is a
 * custom-textured {@code PLAYER_HEAD} ({@code FurnaceKitRegistry}) — itself a
 * real, placeable block — so placing it would silently turn a valuable kit
 * into a plain world block, losing its {@code UPGRADE_KIT_TIER} PDC tag for
 * good. Cancelling by PDC tag rather than by material also means any future
 * tier is covered automatically, whatever icon it ends up using.
 */
public final class FurnaceKitProtectionListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (FurnaceKitRegistry.targetTierOf(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }
}
