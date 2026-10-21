package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.api.util.data.StairShape;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightOcclusionTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void complementaryFacesOccludeWithoutEitherBeingAFullFace() {
        BlockState bottom = BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        BlockState top = bottom.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP);

        assertTrue(occludes(bottom, top, Direction.EAST));
        assertTrue(occludes(top, bottom, Direction.WEST));
        assertFalse(occludes(bottom, bottom, Direction.EAST));
        assertFalse(occludes(top, top, Direction.WEST));
        assertTrue(LightOcclusion.affectsLight(REGISTRY, bottom, top));
    }

    @Test
    void stairCornersUseActualGeometryRatherThanOutlineBounds() {
        BlockState straight = BlockTypes.OAK_STAIRS.getDefaultState()
                .withTrait(BlockTraits.DIRECTION, Direction.NORTH)
                .withTrait(BlockTraits.IS_UPSIDE_DOWN, false)
                .withTrait(BlockTraits.STAIR_SHAPE, StairShape.NONE);
        BlockState outer = straight.withTrait(BlockTraits.STAIR_SHAPE, StairShape.OUTER_LEFT);

        assertTrue(occludes(straight, BlockStates.AIR, Direction.NORTH));
        assertTrue(occludes(straight, BlockStates.AIR, Direction.DOWN));
        assertFalse(occludes(straight, BlockStates.AIR, Direction.SOUTH));
        assertFalse(occludes(straight, BlockStates.AIR, Direction.UP));
        assertFalse(occludes(outer, BlockStates.AIR, Direction.NORTH));
        assertTrue(LightOcclusion.affectsLight(REGISTRY, straight, outer));
    }

    @Test
    void transparentFullCubesDoNotBecomeShapeOccluders() {
        assertFalse(occludes(BlockTypes.GLASS.getDefaultState(), BlockStates.AIR, Direction.UP));
        assertFalse(occludes(BlockTypes.ICE.getDefaultState(), BlockStates.AIR, Direction.UP));
        assertTrue(occludes(BlockTypes.FARMLAND.getDefaultState(), BlockStates.AIR, Direction.DOWN));
        assertFalse(occludes(BlockTypes.FARMLAND.getDefaultState(), BlockStates.AIR, Direction.UP));
    }

    @Test
    void secondaryLayerFacesAlsoParticipateInOcclusion() {
        BlockState bottom = BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        BlockState top = bottom.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP);
        LightAccess.Sample from = new LightAccess.Sample(BlockStates.AIR, bottom, 14);
        LightAccess.Sample to = new LightAccess.Sample(top, BlockStates.AIR, 0);

        assertTrue(LightOcclusion.occludes(REGISTRY, from, to, Direction.EAST));
    }

    @Test
    void unrelatedStateChangesDoNotInvalidateIdenticalLightGeometry() {
        BlockState dry = BlockTypes.FARMLAND.getDefaultState().withTrait(BlockTraits.MOISTURIZED_AMOUNT, 0);
        BlockState wet = dry.withTrait(BlockTraits.MOISTURIZED_AMOUNT, 7);

        assertFalse(LightOcclusion.affectsLight(REGISTRY, dry, wet));
    }

    @Test
    void shelvesOnlySealTheirBackAndLecternsDoNotSealTheirTop() {
        BlockState shelf = BlockTypes.OAK_SHELF.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        BlockState lectern = BlockTypes.LECTERN.getDefaultState();

        assertTrue(occludes(shelf, BlockStates.AIR, Direction.SOUTH));
        assertFalse(occludes(shelf, BlockStates.AIR, Direction.NORTH));
        assertFalse(occludes(shelf, BlockStates.AIR, Direction.UP));
        assertTrue(occludes(lectern, BlockStates.AIR, Direction.DOWN));
        assertFalse(occludes(lectern, BlockStates.AIR, Direction.UP));
    }

    private static boolean occludes(BlockState from, BlockState to, Direction direction) {
        return LightOcclusion.occludes(REGISTRY, new LightAccess.Sample(from, BlockStates.AIR, 15),
                new LightAccess.Sample(to, BlockStates.AIR, 0), direction);
    }
}
