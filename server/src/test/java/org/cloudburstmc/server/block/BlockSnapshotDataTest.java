package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.data.Directional;
import org.cloudburstmc.api.block.data.Waterlogged;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockSnapshotDataTest {

    private static final BlockRegistry BLOCKS = BlockTestRegistry.get();

    @Test
    void capturesLayerValuesWithoutRetainingTheInputArray() {
        assertSame(BlockTypes.STONE, BLOCKS.getOrThrow(Identifier.parse("minecraft:stone")));
        BlockState[] layers = {BlockTypes.OAK_STAIRS.getDefaultState(), BlockStates.AIR};
        BlockSnapshot snapshot = new CloudBlockSnapshot(layers);
        layers[0] = BlockStates.STONE;
        layers[1] = BlockStates.WATER;

        assertSame(BlockTypes.OAK_STAIRS, snapshot.getState().getType());
        assertSame(BlockStates.AIR, snapshot.getSecondaryState());
        assertThrows(IllegalArgumentException.class, () -> new CloudBlockSnapshot(new BlockState[]{BlockStates.AIR}));
    }

    @Test
    void waterloggingChangesBothLayersWithoutMutatingTheCapturedBlock() {
        BlockState stairs = BlockTypes.OAK_STAIRS.getDefaultState().withTrait(BlockTraits.DIRECTION, Direction.NORTH);
        BlockSnapshot dry = new CloudBlockSnapshot(stairs, BlockStates.AIR);
        Waterlogged view = dry.getData(Waterlogged.class).orElseThrow();
        assertFalse(view.isWaterlogged());
        assertTrue(stairs.getData(Waterlogged.class).isEmpty());

        BlockSnapshot wet = view.withWaterlogged(true);
        Waterlogged water = wet.getData(Waterlogged.class).orElseThrow();
        assertTrue(water.isWaterlogged());
        assertSame(BlockLayer.SECONDARY, wet.getLiquidLayer());
        assertSame(BlockStates.WATER, wet.getSecondaryState());
        assertSame(stairs, wet.getState());
        assertFalse(dry.containsLiquid());
        assertEquals(Direction.NORTH, wet.getData(Directional.class).orElseThrow().getFacing());

        BlockSnapshot drained = water.withWaterlogged(false);
        assertSame(stairs, drained.getState());
        assertSame(BlockStates.AIR, drained.getSecondaryState());
        assertSame(water.getSnapshot(), water.withWaterlogged(true));
    }

    @Test
    void rejectsNonContainersRequiredWaterAndOtherSecondaryContents() {
        assertTrue(new CloudBlockSnapshot(BlockStates.STONE, BlockStates.AIR).getData(Waterlogged.class).isEmpty());
        assertTrue(new CloudBlockSnapshot(BlockStates.WATER, BlockStates.AIR).getData(Waterlogged.class).isEmpty());
        assertTrue(new CloudBlockSnapshot(BlockTypes.KELP.getDefaultState(), BlockStates.WATER).getData(Waterlogged.class).isEmpty());
        assertTrue(new CloudBlockSnapshot(BlockTypes.BUBBLE_COLUMN.getDefaultState(), BlockStates.WATER).getData(Waterlogged.class).isEmpty());
        assertTrue(new CloudBlockSnapshot(BlockTypes.OAK_STAIRS.getDefaultState(), BlockStates.LAVA).getData(Waterlogged.class).isEmpty());
    }

    @Test
    void waterDependentTraitsUseTheSharedLayerRules() {
        BlockState dryPickle = BlockTypes.SEA_PICKLE.getDefaultState().withTrait(BlockTraits.IS_DEAD, true);
        BlockSnapshot dry = new CloudBlockSnapshot(dryPickle, BlockStates.AIR);
        BlockSnapshot wet = dry.getData(Waterlogged.class).orElseThrow().withWaterlogged(true);
        assertFalse(wet.getState().ensureTrait(BlockTraits.IS_DEAD));

        BlockSnapshot drained = wet.getData(Waterlogged.class).orElseThrow().withWaterlogged(false);
        assertTrue(drained.getState().ensureTrait(BlockTraits.IS_DEAD));
        assertSame(BlockStates.AIR, drained.getSecondaryState());
        assertTrue(dry.getState().ensureTrait(BlockTraits.IS_DEAD));
    }
}
