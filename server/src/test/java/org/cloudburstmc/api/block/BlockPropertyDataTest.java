package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.CrackedState;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.api.util.data.TurtleEggCount;
import org.cloudburstmc.server.block.CloudBlockSnapshot;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockPropertyDataTest {

    @BeforeAll
    static void initializeRegistry() {
        assertSame(BlockRegistry.get(), BlockTestRegistry.get());
    }

    @Test
    void levelsUseTheCompleteNativeDomainAndPreserveOtherProperties() {
        List<IntegerBlockTrait> traits = List.of(BlockTraits.LIQUID_DEPTH, BlockTraits.FILL_LEVEL, BlockTraits.COMPOSTER_FILL_LEVEL);

        for (BlockType type : BlockTypes.values()) {
            for (IntegerBlockTrait trait : traits) {
                if (!type.getTraits().contains(trait)) {
                    continue;
                }

                for (BlockState state : type.getStates()) {
                    Levelled view = state.getData(Levelled.class).orElseThrow();
                    assertEquals(state.ensureTrait(trait).intValue(), view.getLevel());
                    assertEquals(trait.getRange().getStart(), view.getMinimumLevel());
                    assertEquals(trait.getRange().getEnd(), view.getMaximumLevel());

                    for (int level : trait.getPossibleValues()) {
                        assertSame(state.withTrait(trait, level), view.withLevel(level));
                    }

                    assertThrows(IllegalArgumentException.class, () -> view.withLevel(view.getMinimumLevel() - 1));
                    assertThrows(IllegalArgumentException.class, () -> view.withLevel(view.getMaximumLevel() + 1));
                }
            }
        }

        assertEquals(15, BlockTypes.WATER.getDefaultState().getData(Levelled.class).orElseThrow().getMaximumLevel());
        assertEquals(6, BlockTypes.CAULDRON.getDefaultState().getData(Levelled.class).orElseThrow().getMaximumLevel());
        assertEquals(8, BlockTypes.COMPOSTER.getDefaultState().getData(Levelled.class).orElseThrow().getMaximumLevel());

        BlockState ambiguous = BlockRegistry.get().createType(Identifier.parse("test:ambiguous_levels"),
                BlockTraits.FILL_LEVEL, BlockTraits.COMPOSTER_FILL_LEVEL).getDefaultState();
        assertTrue(ambiguous.getData(Levelled.class).isEmpty());
    }

    @Test
    void attachmentAndHangingChangesPreserveCanonicalStates() {
        for (BlockType type : BlockTypes.values()) {
            for (BlockState state : type.getStates()) {
                if (type.getTraits().contains(BlockTraits.IS_ATTACHED)) {
                    Attachable view = state.getData(Attachable.class).orElseThrow();
                    assertEquals(state.ensureTrait(BlockTraits.IS_ATTACHED), view.isAttached());
                    assertSame(state.withTrait(BlockTraits.IS_ATTACHED, true), view.withAttached(true));
                    assertSame(state.withTrait(BlockTraits.IS_ATTACHED, false), view.withAttached(false));
                }

                if (type.getTraits().contains(BlockTraits.IS_HANGING) && !type.getTraits().contains(BlockTraits.BRUSHED_PROGRESS)) {
                    Hangable view = state.getData(Hangable.class).orElseThrow();
                    assertEquals(state.ensureTrait(BlockTraits.IS_HANGING), view.isHanging());
                    assertSame(state.withTrait(BlockTraits.IS_HANGING, true), view.withHanging(true));
                    assertSame(state.withTrait(BlockTraits.IS_HANGING, false), view.withHanging(false));
                }
            }
        }

        assertTrue(BlockTypes.TRIPWIRE_HOOK.getDefaultState().getData(Attachable.class).isPresent());
        assertTrue(BlockTypes.OAK_HANGING_SIGN.getDefaultState().getData(Attachable.class).isPresent());
        assertTrue(BlockTypes.LANTERN.getDefaultState().getData(Hangable.class).isPresent());
        assertTrue(BlockTypes.MANGROVE_PROPAGULE.getDefaultState().getData(Hangable.class).isPresent());
        assertTrue(BlockTypes.SUSPICIOUS_SAND.getDefaultState().getData(Hangable.class).isEmpty());
    }

    @Test
    void hatchStagesDoNotChangeTheEggCount() {
        List<CrackedState> stages = List.of(CrackedState.NO_CRACKS, CrackedState.CRACKED, CrackedState.MAX_CRACKED);

        for (BlockType type : List.of(BlockTypes.SNIFFER_EGG, BlockTypes.TURTLE_EGG)) {
            for (BlockState state : type.getStates()) {
                Hatchable view = state.getData(Hatchable.class).orElseThrow();
                assertEquals(2, view.getMaximumHatch());
                assertEquals(stages.indexOf(state.ensureTrait(BlockTraits.CRACKED_STATE)), view.getHatch());

                for (int hatch = 0; hatch <= view.getMaximumHatch(); hatch++) {
                    assertSame(state.withTrait(BlockTraits.CRACKED_STATE, stages.get(hatch)), view.withHatch(hatch));
                }

                assertThrows(IllegalArgumentException.class, () -> view.withHatch(-1));
                assertThrows(IllegalArgumentException.class, () -> view.withHatch(3));
                assertThrows(IllegalArgumentException.class, () -> view.withHatch(Integer.MAX_VALUE));
            }
        }
    }

    @Test
    void everyCandleColourExposesOneBasedCountsAndLighting() {
        int colours = 0;

        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().contains(BlockTraits.CANDLES)) {
                continue;
            }

            colours++;
            for (BlockState state : type.getStates()) {
                Candle view = state.getData(Candle.class).orElseThrow();
                assertEquals(state.ensureTrait(BlockTraits.CANDLES) + 1, view.getCandles());
                assertEquals(1, view.getMinimumCandles());
                assertEquals(4, view.getMaximumCandles());
                assertEquals(state.ensureTrait(BlockTraits.IS_LIT), view.isLit());
                assertSame(state.withTrait(BlockTraits.IS_LIT, true), view.withLit(true));
                assertSame(state.withTrait(BlockTraits.IS_LIT, false), view.withLit(false));

                for (int candles = view.getMinimumCandles(); candles <= view.getMaximumCandles(); candles++) {
                    assertSame(state.withTrait(BlockTraits.CANDLES, candles - 1), view.withCandles(candles));
                }

                assertThrows(IllegalArgumentException.class, () -> view.withCandles(0));
                assertThrows(IllegalArgumentException.class, () -> view.withCandles(5));
                assertThrows(IllegalArgumentException.class, () -> view.withCandles(Integer.MIN_VALUE));
            }
        }

        assertEquals(17, colours);
        assertTrue(BlockTypes.CANDLE_CAKE.getDefaultState().getData(Candle.class).isEmpty());
    }

    @Test
    void pickleCountsPreserveTheDryState() {
        for (BlockState state : BlockTypes.SEA_PICKLE.getStates()) {
            SeaPickle view = state.getData(SeaPickle.class).orElseThrow();
            assertEquals(state.ensureTrait(BlockTraits.CLUSTER_COUNT) + 1, view.getPickles());
            assertEquals(1, view.getMinimumPickles());
            assertEquals(4, view.getMaximumPickles());

            for (int count = view.getMinimumPickles(); count <= view.getMaximumPickles(); count++) {
                assertSame(state.withTrait(BlockTraits.CLUSTER_COUNT, count - 1), view.withPickles(count));
            }

            assertThrows(IllegalArgumentException.class, () -> view.withPickles(0));
            assertThrows(IllegalArgumentException.class, () -> view.withPickles(5));
            assertThrows(IllegalArgumentException.class, () -> view.withPickles(Integer.MIN_VALUE));
        }
    }

    @Test
    void turtleEggCountsAndHatchStagesRoundTripIndependently() {
        List<TurtleEggCount> counts = List.of(TurtleEggCount.ONE_EGG, TurtleEggCount.TWO_EGG,
                TurtleEggCount.THREE_EGG, TurtleEggCount.FOUR_EGG);

        for (BlockState state : BlockTypes.TURTLE_EGG.getStates()) {
            TurtleEgg view = state.getData(TurtleEgg.class).orElseThrow();
            assertEquals(counts.indexOf(state.ensureTrait(BlockTraits.TURTLE_EGG_COUNT)) + 1, view.getEggs());
            assertEquals(1, view.getMinimumEggs());
            assertEquals(4, view.getMaximumEggs());
            assertEquals(state.getData(Hatchable.class).orElseThrow().getHatch(), view.getHatch());
            assertEquals(2, view.getMaximumHatch());

            for (int eggs = view.getMinimumEggs(); eggs <= view.getMaximumEggs(); eggs++) {
                assertSame(state.withTrait(BlockTraits.TURTLE_EGG_COUNT, counts.get(eggs - 1)), view.withEggs(eggs));
            }

            for (int hatch = 0; hatch <= view.getMaximumHatch(); hatch++) {
                assertSame(state.getData(Hatchable.class).orElseThrow().withHatch(hatch), view.withHatch(hatch));
            }

            assertThrows(IllegalArgumentException.class, () -> view.withEggs(0));
            assertThrows(IllegalArgumentException.class, () -> view.withEggs(5));
            assertThrows(IllegalArgumentException.class, () -> view.withHatch(-1));
            assertThrows(IllegalArgumentException.class, () -> view.withHatch(3));
        }
    }

    @Test
    void snowLayersPreserveTheCoverFlag() {
        for (BlockState state : BlockTypes.SNOW_LAYER.getStates()) {
            Snow view = state.getData(Snow.class).orElseThrow();
            assertEquals(state.ensureTrait(BlockTraits.HEIGHT) + 1, view.getLayers());
            assertEquals(1, view.getMinimumLayers());
            assertEquals(8, view.getMaximumLayers());

            for (int layers = view.getMinimumLayers(); layers <= view.getMaximumLayers(); layers++) {
                assertSame(state.withTrait(BlockTraits.HEIGHT, layers - 1), view.withLayers(layers));
            }

            assertThrows(IllegalArgumentException.class, () -> view.withLayers(0));
            assertThrows(IllegalArgumentException.class, () -> view.withLayers(9));
        }

        assertTrue(BlockTypes.SNOW.getDefaultState().getData(Snow.class).isEmpty());
    }

    @Test
    void speleothemDirectionAndThicknessPreserveAllOtherTraits() {
        for (BlockType type : List.of(BlockTypes.POINTED_DRIPSTONE, BlockTypes.SULFUR_SPIKE, BlockTypes.ICICLE)) {
            for (BlockState state : type.getStates()) {
                Speleothem view = state.getData(Speleothem.class).orElseThrow();
                Direction direction = state.ensureTrait(BlockTraits.IS_HANGING) ? Direction.DOWN : Direction.UP;
                assertEquals(direction, view.getVerticalDirection());
                assertEquals(Set.of(Direction.UP, Direction.DOWN), view.getVerticalDirections());
                assertEquals(state.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS), view.getThickness());
                assertSame(state.withTrait(BlockTraits.IS_HANGING, true), view.withVerticalDirection(Direction.DOWN));
                assertSame(state.withTrait(BlockTraits.IS_HANGING, false), view.withVerticalDirection(Direction.UP));

                for (DripstoneThickness thickness : DripstoneThickness.values()) {
                    assertSame(state.withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness), view.withThickness(thickness));
                }

                assertThrows(IllegalArgumentException.class, () -> view.withVerticalDirection(Direction.NORTH));
                assertThrows(NullPointerException.class, () -> view.withVerticalDirection(null));
                assertThrows(NullPointerException.class, () -> view.withThickness(null));
            }
        }
    }

    @Test
    void snapshotsExposeCountViewsWithoutDroppingWaterlogging() {
        BlockSnapshot wet = new CloudBlockSnapshot(BlockTypes.SEA_PICKLE.getDefaultState(), BlockStates.WATER);
        SeaPickle count = wet.getData(SeaPickle.class).orElseThrow();
        BlockSnapshot changed = new CloudBlockSnapshot(count.withPickles(4), wet.getSecondaryState());

        assertTrue(changed.getData(Waterlogged.class).orElseThrow().isWaterlogged());
        assertSame(BlockStates.WATER, changed.getSecondaryState());
        assertEquals(1, count.getPickles());
        assertEquals(4, changed.getData(SeaPickle.class).orElseThrow().getPickles());
    }

    @Test
    void unrelatedBlocksDoNotAcquirePropertyViews() {
        List<Class<? extends BlockData>> views = List.of(Levelled.class, Attachable.class, Hangable.class,
                Hatchable.class, Candle.class, SeaPickle.class, TurtleEgg.class, Snow.class, Speleothem.class);

        for (Class<? extends BlockData> view : views) {
            assertTrue(BlockStates.STONE.getData(view).isEmpty());
        }

        assertTrue(BlockTypes.GRASS_BLOCK.getDefaultState().getData(Snow.class).isEmpty());
        assertTrue(BlockTypes.SNIFFER_EGG.getDefaultState().getData(TurtleEgg.class).isEmpty());
    }
}
