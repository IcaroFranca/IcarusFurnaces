package dev.icaro.icarusfurnaces.tier;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;

import java.util.Locale;
import java.util.Optional;

/**
 * A tier of upgraded furnace, ordered from slowest to fastest. The ordinal of
 * each constant is persisted (as PDC on the placed furnace's tile entity and
 * on every kit item), so constants must never be reordered — only appended
 * at the end.
 *
 * <p>Unlike {@code ChestTier} there is no explicit "tier zero" constant: a
 * freshly placed vanilla furnace simply carries no {@code FURNACE_TIER} tag
 * at all (see {@code FurnaceTierService}) until any kit is applied to it —
 * any tier's kit works on any furnace regardless of its current tier, there
 * is no sequential-progression requirement (see {@code
 * FurnaceInteractListener}). A furnace's capacity (3 slots: input, fuel,
 * output) never changes between tiers either — only {@link #cookTicks()}
 * does — so applying a kit here never needs to resize anything.
 */
public enum FurnaceTier {

    COPPER("Cobre", 180, TextColor.color(0xC8, 0x71, 0x37)),
    IRON("Ferro", 160, TextColor.color(0xDC, 0xDC, 0xDC)),
    GOLD("Ouro", 120, TextColor.color(0xFF, 0xD9, 0x66)),
    DIAMOND("Diamante", 80, TextColor.color(0x4A, 0xED, 0xD9)),
    EMERALD("Esmeralda", 40, TextColor.color(0x47, 0xE0, 0x59)),
    OBSIDIAN("Obsidiana", 20, TextColor.color(0x7A, 0x5C, 0xC9)),
    NETHERITE("Netherite", 5, TextColor.color(0x6E, 0x5A, 0x61));

    private final String displayName;
    private final int cookTicks;
    private final TextColor titleColor;

    FurnaceTier(String displayName, int cookTicks, TextColor titleColor) {
        if (cookTicks <= 0) {
            throw new IllegalArgumentException("cookTicks must be positive: " + cookTicks);
        }
        this.displayName = displayName;
        this.cookTicks = cookTicks;
        this.titleColor = titleColor;
    }

    public String displayName() {
        return displayName;
    }

    /** Ticks {@code FurnaceStartSmeltEvent#setTotalCookTime} is set to for a furnace at this tier. Vanilla default is 200. */
    public int cookTicks() {
        return cookTicks;
    }

    /** Color used for this tier's name, matching its material — in the furnace's custom container title and in item lore. */
    public TextColor titleColor() {
        return titleColor;
    }

    /** {@link #titleColor()}, same RGB, as a {@link Color} for {@code Particle.DustOptions} — see {@code FurnaceParticleListener}. */
    public Color particleColor() {
        return Color.fromRGB(titleColor.red(), titleColor.green(), titleColor.blue());
    }

    /**
     * The tier one step below this one in the Cobre→Netherite ordering, if any (Copper has none).
     * Plain ordinal navigation — any kit can be applied to any furnace regardless of its current
     * tier (see {@code FurnaceInteractListener}), so this isn't tied to any gameplay gate anymore.
     */
    public Optional<FurnaceTier> previous() {
        int previousOrdinal = ordinal() - 1;
        return previousOrdinal >= 0 ? Optional.of(values()[previousOrdinal]) : Optional.empty();
    }

    /** The tier one step above this one in the Cobre→Netherite ordering, if any — see {@link #previous()}. */
    public Optional<FurnaceTier> next() {
        FurnaceTier[] values = values();
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal < values.length ? Optional.of(values[nextOrdinal]) : Optional.empty();
    }

    public static Optional<FurnaceTier> byOrdinal(int ordinal) {
        FurnaceTier[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return Optional.empty();
        }
        return Optional.of(values[ordinal]);
    }

    /** Namespaced key suffix conventionally used for this tier's upgrade kit recipe, e.g. {@code "furnace_upgrade_kit_copper"}. */
    public String upgradeKitKey() {
        return "furnace_upgrade_kit_" + name().toLowerCase(Locale.ROOT);
    }

    /** Reverses {@link #upgradeKitKey()} — the tier a registered recipe's key belongs to, if it's one of ours. */
    public static Optional<FurnaceTier> fromUpgradeKitKey(String recipeKey) {
        String prefix = "furnace_upgrade_kit_";
        if (recipeKey == null || !recipeKey.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(recipeKey.substring(prefix.length()).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
