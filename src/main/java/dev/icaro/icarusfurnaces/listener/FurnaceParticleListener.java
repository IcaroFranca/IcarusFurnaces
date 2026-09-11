package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.tier.FurnaceTierService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;

import java.util.Optional;

/**
 * Purely cosmetic: a puff of {@link Particle#DUST}, colored with the tier's
 * own {@link FurnaceTier#particleColor()}, every time an upgraded furnace
 * starts smelting an item or ignites a new piece of fuel. Without this, a
 * tier is otherwise invisible from outside — the only other tell is the
 * furnace's GUI title, and that needs the GUI open to see at all.
 *
 * <p>Deliberately hooks events that already fire at each tier's own pace
 * instead of a fixed scheduler tick over every tagged furnace (which would
 * need its own location registry — this plugin keeps none, see {@code
 * FurnaceTierService}): a fast tier starts a new item that much more often,
 * so it sparks far more frequently than a slow one for free, reinforcing the
 * speed difference visually with no extra bookkeeping. A plain, never
 * upgraded furnace never spawns anything here — same guard {@code
 * FurnaceCookSpeedListener} already uses.
 */
public final class FurnaceParticleListener implements Listener {

    private static final int SMELT_PARTICLE_COUNT = 6;
    private static final int BURN_PARTICLE_COUNT = 10;
    private static final float DUST_SIZE = 1.2f;

    @EventHandler
    public void onStartSmelt(FurnaceStartSmeltEvent event) {
        spawnBurst(event.getBlock(), SMELT_PARTICLE_COUNT, 0.9);
    }

    /** A slightly bigger puff when fuel ignites — reads as "the fire itself" catching the tier's color. */
    @EventHandler
    public void onBurn(FurnaceBurnEvent event) {
        spawnBurst(event.getBlock(), BURN_PARTICLE_COUNT, 0.6);
    }

    private void spawnBurst(Block block, int count, double yOffset) {
        if (block.getType() != Material.FURNACE) {
            return;
        }
        Optional<FurnaceTier> tier = FurnaceTierService.tierOf(block);
        if (tier.isEmpty()) {
            return;
        }
        Location location = block.getLocation().add(0.5, yOffset, 0.5);
        Particle.DustOptions dust = new Particle.DustOptions(tier.get().particleColor(), DUST_SIZE);
        block.getWorld().spawnParticle(Particle.DUST, location, count, 0.25, 0.15, 0.25, 0, dust);
    }
}
