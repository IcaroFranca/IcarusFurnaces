package dev.icaro.icarusfurnaces.listener;

import dev.icaro.icarusfurnaces.tier.FurnaceTier;
import dev.icaro.icarusfurnaces.tier.FurnaceTierService;
import dev.icaro.icarusfurnaces.upgrade.FurnaceKitRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Applies an upgrade kit when a player shift-right-clicks a placed Furnace while holding one — any
 * kit works on any furnace, setting it straight to that kit's tier regardless of what tier it was
 * at before (skipping tiers, or even "downgrading", both allowed on purpose — there is no
 * sequential-progression gate here at all, only "does this kit already match the furnace's current
 * tier" to avoid wasting one on a no-op). Every other click on a furnace is left completely alone —
 * the vanilla furnace GUI opens exactly as it always does, since a furnace's own 3 slots
 * (input/fuel/output) never change between tiers and need no custom inventory at all.
 */
public final class FurnaceInteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            // The hand check avoids handling this event twice per click
            // (Bukkit fires it once per hand for a single right-click).
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FURNACE) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return; // not an upgrade attempt: let the vanilla furnace GUI open normally
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        Optional<FurnaceTier> kitTarget = FurnaceKitRegistry.targetTierOf(inHand);
        if (kitTarget.isEmpty()) {
            return; // shift-clicking without a kit in hand is still just "open the furnace"
        }

        event.setCancelled(true);
        applyKit(player, block, inHand, kitTarget.get());
    }

    private void applyKit(Player player, Block block, ItemStack kit, FurnaceTier kitTarget) {
        Optional<FurnaceTier> current = FurnaceTierService.tierOf(block);
        if (current.isPresent() && current.get() == kitTarget) {
            player.sendMessage(Component.text("Esta fornalha ja esta no tier " + kitTarget.displayName() + ".", NamedTextColor.RED));
            return; // don't consume the kit on a no-op
        }

        FurnaceTierService.applyTier(block, kitTarget);
        kit.setAmount(kit.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        spawnUpgradeBurst(block, kitTarget);
        player.sendMessage(Component.text("Fornalha ajustada para " + kitTarget.displayName() + "!", NamedTextColor.GREEN));
    }

    /** A bigger, one-off puff of the new tier's color right on upgrade — see {@code FurnaceParticleListener} for the ongoing, per-smelt version. */
    private void spawnUpgradeBurst(Block block, FurnaceTier newTier) {
        Location location = block.getLocation().add(0.5, 1.1, 0.5);
        Particle.DustOptions dust = new Particle.DustOptions(newTier.particleColor(), 1.8f);
        block.getWorld().spawnParticle(Particle.DUST, location, 40, 0.35, 0.35, 0.35, 0, dust);
    }
}
