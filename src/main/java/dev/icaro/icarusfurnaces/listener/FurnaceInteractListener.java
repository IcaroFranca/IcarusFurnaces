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
 * Applies an upgrade kit when a player shift-right-clicks a placed Furnace
 * while holding one that matches its next tier. Every other click on a
 * furnace is left completely alone — the vanilla furnace GUI opens exactly
 * as it always does, since a furnace's own 3 slots (input/fuel/output) never
 * change between tiers and need no custom inventory at all.
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
        // Untagged (never upgraded) means the required next step is Copper; otherwise it's
        // whatever comes after the current tier — empty if already at Netherite.
        Optional<FurnaceTier> requiredNext = current.isPresent() ? current.get().next() : Optional.of(FurnaceTier.COPPER);

        if (requiredNext.isEmpty() || kitTarget != requiredNext.get()) {
            player.sendMessage(Component.text("Este kit nao serve para o proximo tier desta fornalha.", NamedTextColor.RED));
            return;
        }

        FurnaceTierService.applyTier(block, requiredNext.get());
        kit.setAmount(kit.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        spawnUpgradeBurst(block, requiredNext.get());
        player.sendMessage(Component.text("Fornalha evoluida para " + requiredNext.get().displayName() + "!", NamedTextColor.GREEN));
    }

    /** A bigger, one-off puff of the new tier's color right on upgrade — see {@code FurnaceParticleListener} for the ongoing, per-smelt version. */
    private void spawnUpgradeBurst(Block block, FurnaceTier newTier) {
        Location location = block.getLocation().add(0.5, 1.1, 0.5);
        Particle.DustOptions dust = new Particle.DustOptions(newTier.particleColor(), 1.8f);
        block.getWorld().spawnParticle(Particle.DUST, location, 40, 0.35, 0.35, 0.35, 0, dust);
    }
}
