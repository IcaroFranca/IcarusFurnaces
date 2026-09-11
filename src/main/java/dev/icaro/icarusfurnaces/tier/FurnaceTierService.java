package dev.icaro.icarusfurnaces.tier;

import dev.icaro.icarusfurnaces.util.NamespacedKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Nameable;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Reads and writes a placed furnace's {@link FurnaceTier} — tagged directly
 * on the block's own tile-entity {@code PersistentDataContainer}, which the
 * server already persists as part of the chunk's NBT automatically. Unlike
 * IcarusChests' chests, a furnace's tier needs no database row at all: the
 * only per-tier state (cook speed) is derived fresh from the tag every time
 * a smelt starts (see {@code FurnaceCookSpeedListener}), and the 3 vanilla
 * slots (input/fuel/output) never change shape between tiers.
 */
public final class FurnaceTierService {

    private FurnaceTierService() {
    }

    /** The tier tagged on {@code block}, if it's a furnace that was ever upgraded at least once. */
    public static Optional<FurnaceTier> tierOf(Block block) {
        if (!(block.getState() instanceof TileState state)) {
            return Optional.empty();
        }
        Integer ordinal = state.getPersistentDataContainer().get(NamespacedKeys.FURNACE_TIER, PersistentDataType.INTEGER);
        return ordinal == null ? Optional.empty() : FurnaceTier.byOrdinal(ordinal);
    }

    /**
     * Tags {@code block} as {@code tier} and, when the tile state supports it (a plain
     * {@code Furnace} always does), renames its container so the tier shows up as the
     * vanilla GUI's own title — no custom inventory needed for that.
     */
    public static void applyTier(Block block, FurnaceTier tier) {
        if (!(block.getState() instanceof TileState state)) {
            return;
        }
        state.getPersistentDataContainer().set(NamespacedKeys.FURNACE_TIER, PersistentDataType.INTEGER, tier.ordinal());
        if (state instanceof Nameable nameable) {
            nameable.customName(Component.text("Fornalha de " + tier.displayName(), tier.titleColor()));
        }
        state.update(true);
    }
}
