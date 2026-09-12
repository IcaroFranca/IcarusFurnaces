package dev.icaro.icarusfurnaces.tier;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnaceTierTest {

    @Test
    void cookTicksMatchTheDesignedProgressionWorstToBest() {
        assertEquals(180, FurnaceTier.COPPER.cookTicks());
        assertEquals(160, FurnaceTier.IRON.cookTicks());
        assertEquals(120, FurnaceTier.GOLD.cookTicks());
        assertEquals(80, FurnaceTier.DIAMOND.cookTicks());
        assertEquals(40, FurnaceTier.EMERALD.cookTicks());
        assertEquals(20, FurnaceTier.OBSIDIAN.cookTicks());
        assertEquals(5, FurnaceTier.NETHERITE.cookTicks());
    }

    @Test
    void everyTierIsStrictlyFasterThanTheOneBeforeIt() {
        FurnaceTier[] values = FurnaceTier.values();
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i].cookTicks() < values[i - 1].cookTicks(),
                    values[i] + " must smelt faster (fewer ticks) than " + values[i - 1]);
        }
    }

    @Test
    void everyTierIsFasterThanVanillasDefaultFurnace() {
        int vanillaCookTicks = 200;
        for (FurnaceTier tier : FurnaceTier.values()) {
            assertTrue(tier.cookTicks() < vanillaCookTicks, tier + " should beat vanilla's " + vanillaCookTicks + " ticks");
        }
    }

    @Test
    void copperHasNoPreviousTierButEveryOtherTierDoes() {
        assertTrue(FurnaceTier.COPPER.previous().isEmpty());
        for (FurnaceTier tier : FurnaceTier.values()) {
            if (tier == FurnaceTier.COPPER) {
                continue;
            }
            assertTrue(tier.previous().isPresent(), tier + " should have a tier ordered just below it");
        }
    }

    @Test
    void nextNavigatesTheOrdinalOrdering() {
        assertEquals(Optional.of(FurnaceTier.IRON), FurnaceTier.COPPER.next());
        assertEquals(Optional.of(FurnaceTier.GOLD), FurnaceTier.IRON.next());
        assertEquals(Optional.of(FurnaceTier.DIAMOND), FurnaceTier.GOLD.next());
        assertEquals(Optional.of(FurnaceTier.EMERALD), FurnaceTier.DIAMOND.next());
        assertEquals(Optional.of(FurnaceTier.OBSIDIAN), FurnaceTier.EMERALD.next());
        assertEquals(Optional.of(FurnaceTier.NETHERITE), FurnaceTier.OBSIDIAN.next());
        assertTrue(FurnaceTier.NETHERITE.next().isEmpty(), "Netherite is the max tier, it should have no next()");
    }

    @Test
    void byOrdinalRoundTripsForEveryTier() {
        for (FurnaceTier tier : FurnaceTier.values()) {
            assertEquals(Optional.of(tier), FurnaceTier.byOrdinal(tier.ordinal()));
        }
        assertTrue(FurnaceTier.byOrdinal(-1).isEmpty());
        assertTrue(FurnaceTier.byOrdinal(FurnaceTier.values().length).isEmpty());
    }

    @Test
    void upgradeKitKeyIsLowercaseUniqueAndRoundTrips() {
        long distinctKeys = Arrays.stream(FurnaceTier.values())
                .map(FurnaceTier::upgradeKitKey)
                .distinct()
                .count();
        assertEquals(FurnaceTier.values().length, distinctKeys, "every tier must have a unique upgrade kit key");
        assertFalse(FurnaceTier.COPPER.upgradeKitKey().contains(" "), "recipe keys can't contain spaces");

        for (FurnaceTier tier : FurnaceTier.values()) {
            assertEquals(Optional.of(tier), FurnaceTier.fromUpgradeKitKey(tier.upgradeKitKey()));
        }
        assertTrue(FurnaceTier.fromUpgradeKitKey("not_a_real_key").isEmpty());
        assertTrue(FurnaceTier.fromUpgradeKitKey(null).isEmpty());
    }

    @Test
    void everyTierHasADistinctTitleColor() {
        long distinctColors = Arrays.stream(FurnaceTier.values())
                .map(FurnaceTier::titleColor)
                .distinct()
                .count();
        assertEquals(FurnaceTier.values().length, distinctColors, "every tier should have its own title color");
    }

    @Test
    void particleColorMatchesTitleColorsRgb() {
        // FurnaceParticleListener/FurnaceInteractListener spawn Particle.DustOptions from this —
        // it must always agree with the color already shown in the furnace's GUI title/item lore.
        for (FurnaceTier tier : FurnaceTier.values()) {
            assertEquals(tier.titleColor().red(), tier.particleColor().getRed(), tier + "'s particle red channel must match its title color");
            assertEquals(tier.titleColor().green(), tier.particleColor().getGreen(), tier + "'s particle green channel must match its title color");
            assertEquals(tier.titleColor().blue(), tier.particleColor().getBlue(), tier + "'s particle blue channel must match its title color");
        }
    }
}
