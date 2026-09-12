package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.tier.FurnaceTierService;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Optional;

/**
 * Refunds every upgrade kit consumed to reach a furnace's current tier when it's destroyed —
 * broken by a player, or caught in an explosion — so upgrading a furnace is never a one-way,
 * unrefundable material sink. Tier is sequential and gate-checked (see {@code
 * FurnaceInteractListener}): reaching tier N always means kits for every tier from {@link
 * FurnaceTier#COPPER} up through N were consumed, one each, so this drops all of them, not just
 * the one for the current tier. A plain, never-upgraded furnace has nothing to refund and is left
 * completely alone (drops normally, vanilla-only, via the game's own break/explosion handling —
 * nothing here needs to touch that).
 */
public final class FurnaceBreakListener implements Listener {

    private final FurnaceKitRegistry furnaceKitRegistry;

    public FurnaceBreakListener(FurnaceKitRegistry furnaceKitRegistry) {
        this.furnaceKitRegistry = furnaceKitRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        refundKits(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            refundKits(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            refundKits(block);
        }
    }

    private void refundKits(Block block) {
        if (block.getType() != Material.FURNACE) {
            return;
        }
        Optional<FurnaceTier> tier = FurnaceTierService.tierOf(block);
        if (tier.isEmpty()) {
            return; // never upgraded — nothing was ever spent on it
        }

        World world = block.getWorld();
        for (FurnaceTier step : FurnaceTier.values()) {
            world.dropItemNaturally(block.getLocation(), furnaceKitRegistry.createKit(step));
            if (step == tier.get()) {
                break;
            }
        }
    }
}
