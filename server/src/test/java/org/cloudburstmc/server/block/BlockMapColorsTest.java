package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockMapColorsTest {

    @Test
    void resolvesTransparentTintedTerrainColors() {
        assertEquals(0xff4040ff, BlockMapColors.resolve("#00000000", "Water", BlockTypes.WATER).getRGB());
        assertEquals(0xff7fb238, BlockMapColors.resolve("#00000000", "Grass", BlockTypes.GRASS_BLOCK).getRGB());
        assertEquals(0xff007c00, BlockMapColors.resolve("#00000000", "Grass", BlockTypes.SHORT_GRASS).getRGB());
        assertEquals(0xff007c00, BlockMapColors.resolve("#00000000", "DefaultFoliage", BlockTypes.OAK_LEAVES).getRGB());
    }

    @Test
    void preservesExplicitColorsAndUntintedTransparency() {
        assertEquals(0xff976d4d, BlockMapColors.resolve("#976d4dff", "None", BlockTypes.DIRT).getRGB());
        assertEquals(0, BlockMapColors.resolve("#00000000", "None", BlockTypes.AIR).getRGB());
        assertEquals(0, BlockMapColors.resolve("#00000000", "RedStoneWire", BlockTypes.REDSTONE_WIRE).getRGB());
    }
}
