package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.BlockType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class VanillaCopperBlocksTest {

    @Test
    void pairsEveryWaxedCatalogBlockWithItsUnwaxedVariant() {
        List<BlockType> waxedTypes = values().stream()
                .filter(type -> type.getId().getName().startsWith("waxed_"))
                .toList();

        for (BlockType waxed : waxedTypes) {
            BlockType source = VanillaCopperBlocks.unwaxed(waxed);
            assertNotNull(source, waxed.toString());
            assertSame(waxed, VanillaCopperBlocks.waxed(source));
            assertEquals(-1, VanillaCopperBlocks.weatheringStage(waxed));
            assertNull(VanillaCopperBlocks.next(waxed));
            assertNull(VanillaCopperBlocks.previous(waxed));
        }
    }

    @Test
    void weatheringAndScrapingMoveExactlyOneStage() {
        assertSame(EXPOSED_COPPER, VanillaCopperBlocks.next(COPPER_BLOCK));
        assertSame(COPPER_BLOCK, VanillaCopperBlocks.previous(EXPOSED_COPPER));
        assertNull(VanillaCopperBlocks.previous(COPPER_BLOCK));
        assertNull(VanillaCopperBlocks.next(OXIDIZED_COPPER));

        for (BlockType source : values()) {
            BlockType next = VanillaCopperBlocks.next(source);
            if (next != null) {
                assertSame(source, VanillaCopperBlocks.previous(next));
                assertEquals(VanillaCopperBlocks.weatheringStage(source) + 1, VanillaCopperBlocks.weatheringStage(next));
                assertEquals(source.getDefaultState().getTraits().keySet(), next.getDefaultState().getTraits().keySet());
            }
        }
    }

    @Test
    void lanternFamilyContainsOnlyLanternVariants() {
        assertEquals(List.of(
                COPPER_LANTERN, WAXED_COPPER_LANTERN,
                EXPOSED_COPPER_LANTERN, WAXED_EXPOSED_COPPER_LANTERN,
                WEATHERED_COPPER_LANTERN, WAXED_WEATHERED_COPPER_LANTERN,
                OXIDIZED_COPPER_LANTERN, WAXED_OXIDIZED_COPPER_LANTERN
        ), VanillaCopperBlocks.family(COPPER_LANTERN));
    }
}
