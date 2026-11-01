package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.api.util.data.StairShape;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LightStateCacheTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void fastOcclusionMatchesShapeOcclusion() {
        LightStateCache cache = new LightStateCache(REGISTRY);
        List<StatePair> samples = new ArrayList<>();
        for (BlockState state : states()) {
            samples.add(new StatePair(state, BlockStates.AIR));
            samples.add(new StatePair(BlockStates.AIR, state));
        }

        for (StatePair from : samples) {
            for (StatePair to : samples) {
                for (Direction direction : Direction.values()) {
                    assertEquals(LightOcclusion.occludes(REGISTRY, from.primary(), from.secondary(),
                                    to.primary(), to.secondary(), direction),
                            cache.occludes(from.primary(), from.secondary(), to.primary(), to.secondary(), direction),
                            () -> "occludes " + from.primary() + "," + from.secondary()
                                    + " -> " + to.primary() + "," + to.secondary() + " " + direction);
                }
            }
        }
    }

    @Test
    void opacityAndEmissionTakeTheMaxOfBothLayers() {
        LightStateCache cache = new LightStateCache(REGISTRY);
        BlockState water = BlockTypes.WATER.getDefaultState();
        assertEquals(water.getLightDampening(), cache.opacity(BlockStates.AIR, water));
        assertEquals(water.getLightDampening(), cache.opacity(water, BlockStates.AIR));

        BlockState lava = BlockTypes.LAVA.getDefaultState();
        assertEquals(15, cache.emission(BlockStates.AIR, lava));
        assertEquals(15, cache.emission(lava, BlockStates.AIR));
    }

    private static List<BlockState> states() {        List<BlockState> states = new ArrayList<>(List.of(
                BlockStates.AIR,
                BlockTypes.STONE.getDefaultState(),
                BlockTypes.GLASS.getDefaultState(),
                BlockTypes.WATER.getDefaultState(),
                BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM),
                BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP),
                BlockTypes.FROSTED_ICE.getDefaultState()));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean upsideDown : new boolean[]{false, true}) {
                for (StairShape shape : StairShape.values()) {
                    states.add(BlockTypes.OAK_STAIRS.getDefaultState()
                            .withTrait(BlockTraits.DIRECTION, facing)
                            .withTrait(BlockTraits.IS_UPSIDE_DOWN, upsideDown)
                            .withTrait(BlockTraits.STAIR_SHAPE, shape));
                }
            }
        }
        for (int layers = 1; layers <= 8; layers++) {
            states.add(BlockTypes.SNOW_LAYER.getDefaultState().withTrait(BlockTraits.HEIGHT, layers - 1));
        }
        return states;
    }

    private record StatePair(BlockState primary, BlockState secondary) {
    }
}
