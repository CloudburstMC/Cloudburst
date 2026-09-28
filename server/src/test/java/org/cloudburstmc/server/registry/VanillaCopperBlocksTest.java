package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.BlockType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class VanillaCopperBlocksTest {

    @Test
    void includesEveryWaxedCatalogBlockExactlyOnce() {
        List<BlockType> waxedTypes = values().stream()
                .filter(type -> type.getId().getName().startsWith("waxed_"))
                .toList();
        assertEquals(waxedTypes.size(), VanillaCopperBlocks.WAX_OFF.size());
        for (BlockType waxed : waxedTypes) {
            BlockType source = VanillaCopperBlocks.WAX_OFF.get(waxed);
            assertNotNull(source, waxed.toString());
            assertSame(waxed, VanillaCopperBlocks.WAXABLES.get(source));
        }
    }

    @Test
    void scrapingRemovesExactlyOneStageWithoutRemovingWax() {
        assertSame(WEATHERED_COPPER, VanillaCopperBlocks.PREVIOUS.get(OXIDIZED_COPPER));
        assertSame(EXPOSED_COPPER, VanillaCopperBlocks.PREVIOUS.get(WEATHERED_COPPER));
        assertSame(COPPER_BLOCK, VanillaCopperBlocks.PREVIOUS.get(EXPOSED_COPPER));
        assertFalse(VanillaCopperBlocks.PREVIOUS.containsKey(COPPER_BLOCK));
        assertFalse(VanillaCopperBlocks.PREVIOUS.containsKey(WAXED_OXIDIZED_COPPER));
        VanillaCopperBlocks.PREVIOUS.forEach((source, previous) -> {
            assertTrue(VanillaCopperBlocks.WAXABLES.containsKey(source));
            assertTrue(VanillaCopperBlocks.WAXABLES.containsKey(previous));
            assertEquals(previous.getDefaultState().getTraits().keySet(), source.getDefaultState().getTraits().keySet());
        });
    }

    @Test
    void rejectsIncompleteStageCollections() {
        assertThrows(IllegalArgumentException.class, () -> new CopperBlockFamily(
                List.of(COPPER_BLOCK, EXPOSED_COPPER),
                List.of(WAXED_COPPER, WAXED_EXPOSED_COPPER)
        ));
    }
}
