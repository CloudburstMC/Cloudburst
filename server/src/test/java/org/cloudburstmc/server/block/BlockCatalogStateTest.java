package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockCatalogStateTest {

    @Test
    void everyExpandedStateHasItsOwnRuntimeDefinition() {
        BlockRegistry registry = BlockTestRegistry.get();
        Map<BlockType, Integer> expectedCounts = Map.of(
                BlockTypes.CHORUS_PLANT, 64,
                BlockTypes.FIRE, 512,
                BlockTypes.REDSTONE_WIRE, 1296,
                BlockTypes.DISC_PRESS, 4,
                BlockTypes.ICE_CRYSTAL, 6,
                BlockTypes.ICICLE, 20);

        expectedCounts.forEach((type, count) -> {
            BlockType registeredType = registry.getBlock(type).getType();
            assertEquals(count.intValue(), registeredType.getStates().size(), type.getId().toString());
            for (BlockState state : registeredType.getStates()) {
                CloudBlockDefinition definition = BlockPalette.INSTANCE.getDefinition(state);
                assertSame(state, definition.getCloudState(), state.toString());
                assertSame(state, BlockStateCodec.read(definition.getState()), state.toString());
            }
        });
    }

    @Test
    void pointedBlocksShareTheirBehaviorAndMiningTags() {
        BlockRegistry registry = BlockTestRegistry.get();
        for (BlockType type : Set.of(BlockTypes.POINTED_DRIPSTONE, BlockTypes.SULFUR_SPIKE, BlockTypes.ICICLE)) {
            BlockState state = registry.getBlock(type);
            assertTrue(state.is(BlockTags.SPELEOTHEMS), type.getId().toString());
            assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), type.getId().toString());
        }
        assertTrue(registry.getBlock(BlockTypes.ICE_CRYSTAL).is(BlockTags.MINEABLE_WITH_PICKAXE));
    }

    @Test
    void magmaMeltsIciclesAboveItButOrdinaryStoneDoesNot() {
        BlockRegistry registry = BlockTestRegistry.get();

        assertTrue(registry.getBlock(BlockTypes.MAGMA).is(BlockTags.MELTS_ICICLE_ABOVE));
        assertFalse(registry.getBlock(BlockTypes.STONE).is(BlockTags.MELTS_ICICLE_ABOVE));
    }
}
