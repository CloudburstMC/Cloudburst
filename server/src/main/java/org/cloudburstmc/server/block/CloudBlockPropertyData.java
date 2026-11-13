package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CrackedState;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.api.util.data.TurtleEggCount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Typed views of fill levels, attachments and counted or staged block properties.
 */
@UtilityClass
public class CloudBlockPropertyData {

    private static final List<IntegerBlockTrait> LEVEL_TRAITS = List.of(BlockTraits.LIQUID_DEPTH, BlockTraits.FILL_LEVEL, BlockTraits.COMPOSTER_FILL_LEVEL);
    private static final List<CrackedState> HATCH_STAGES = List.of(CrackedState.NO_CRACKS, CrackedState.CRACKED, CrackedState.MAX_CRACKED);
    private static final List<TurtleEggCount> EGG_COUNTS = List.of(TurtleEggCount.ONE_EGG, TurtleEggCount.TWO_EGG, TurtleEggCount.THREE_EGG, TurtleEggCount.FOUR_EGG);
    private static final Set<Direction> VERTICAL_DIRECTIONS = Set.of(Direction.UP, Direction.DOWN);

    /**
     * @param state    the state to view
     * @param dataType the requested property interface
     * @param <D>      the view type
     * @return the supported view, or empty when unavailable
     */
    public static <D extends BlockData> Optional<D> get(BlockState state, Class<D> dataType) {
        @Nullable BlockData data = null;

        if (dataType == Levelled.class) {
            data = levelled(state);
        } else if (dataType == Attachable.class && state.getTraits().containsKey(BlockTraits.IS_ATTACHED)) {
            data = new AttachedData(state);
        } else if (dataType == Hangable.class && state.getTraits().containsKey(BlockTraits.IS_HANGING) && !state.getTraits().containsKey(BlockTraits.BRUSHED_PROGRESS)) {
            data = new HangingData(state);
        } else if (dataType == Hatchable.class && state.getTraits().containsKey(BlockTraits.CRACKED_STATE)) {
            data = new HatchData(state);
        } else if (dataType == Candle.class && state.getTraits().containsKey(BlockTraits.CANDLES) && state.getTraits().containsKey(BlockTraits.IS_LIT)) {
            data = new CandleData(state);
        } else if (dataType == SeaPickle.class && state.getTraits().containsKey(BlockTraits.CLUSTER_COUNT) && state.getTraits().containsKey(BlockTraits.IS_DEAD)) {
            data = new PickleData(state);
        } else if (dataType == TurtleEgg.class && state.getTraits().containsKey(BlockTraits.TURTLE_EGG_COUNT) && state.getTraits().containsKey(BlockTraits.CRACKED_STATE)) {
            data = new TurtleEggData(state);
        } else if (dataType == Snow.class && state.getTraits().containsKey(BlockTraits.HEIGHT) && state.getTraits().containsKey(BlockTraits.IS_COVERED)) {
            data = new SnowData(state);
        } else if (dataType == Speleothem.class && state.getTraits().containsKey(BlockTraits.DRIPSTONE_THICKNESS) && state.getTraits().containsKey(BlockTraits.IS_HANGING)) {
            data = new SpeleothemData(state);
        }

        return Optional.ofNullable(dataType.cast(data));
    }

    private static @Nullable Levelled levelled(BlockState state) {
        IntegerBlockTrait level = null;

        for (IntegerBlockTrait trait : LEVEL_TRAITS) {
            if (state.getTraits().containsKey(trait)) {
                if (level != null) {
                    return null;
                }

                level = trait;
            }
        }

        return level == null ? null : new LevelData(state, level);
    }

    private static int hatchStage(BlockState state) {
        return HATCH_STAGES.indexOf(state.ensureTrait(BlockTraits.CRACKED_STATE));
    }

    private static BlockState withHatch(BlockState state, int hatch) {
        checkArgument(hatch >= 0 && hatch < HATCH_STAGES.size(), "Unsupported hatch stage: %s", hatch);
        return state.withTrait(BlockTraits.CRACKED_STATE, HATCH_STAGES.get(hatch));
    }

    private static BlockState withCount(BlockState state, IntegerBlockTrait trait, int count) {
        int minimum = trait.getRange().getStart() + 1;
        int maximum = trait.getRange().getEnd() + 1;
        checkArgument(count >= minimum && count <= maximum, "Unsupported block count: %s", count);
        return state.withTrait(trait, count - 1);
    }

    private record LevelData(BlockState state, IntegerBlockTrait trait) implements Levelled {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getLevel() {
            return this.state.ensureTrait(this.trait);
        }

        @Override
        public int getMinimumLevel() {
            return this.trait.getRange().getStart();
        }

        @Override
        public int getMaximumLevel() {
            return this.trait.getRange().getEnd();
        }

        @Override
        public BlockState withLevel(int level) {
            return this.state.withTrait(this.trait, level);
        }
    }

    private record AttachedData(BlockState state) implements Attachable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isAttached() {
            return this.state.ensureTrait(BlockTraits.IS_ATTACHED);
        }

        @Override
        public BlockState withAttached(boolean attached) {
            return this.state.withTrait(BlockTraits.IS_ATTACHED, attached);
        }
    }

    private record HangingData(BlockState state) implements Hangable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isHanging() {
            return this.state.ensureTrait(BlockTraits.IS_HANGING);
        }

        @Override
        public BlockState withHanging(boolean hanging) {
            return this.state.withTrait(BlockTraits.IS_HANGING, hanging);
        }
    }

    private record HatchData(BlockState state) implements Hatchable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getHatch() {
            return hatchStage(this.state);
        }

        @Override
        public int getMaximumHatch() {
            return HATCH_STAGES.size() - 1;
        }

        @Override
        public BlockState withHatch(int hatch) {
            return CloudBlockPropertyData.withHatch(this.state, hatch);
        }
    }

    private record CandleData(BlockState state) implements Candle {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getCandles() {
            return this.state.ensureTrait(BlockTraits.CANDLES) + 1;
        }

        @Override
        public int getMinimumCandles() {
            return BlockTraits.CANDLES.getRange().getStart() + 1;
        }

        @Override
        public int getMaximumCandles() {
            return BlockTraits.CANDLES.getRange().getEnd() + 1;
        }

        @Override
        public BlockState withCandles(int candles) {
            return withCount(this.state, BlockTraits.CANDLES, candles);
        }

        @Override
        public boolean isLit() {
            return this.state.ensureTrait(BlockTraits.IS_LIT);
        }

        @Override
        public BlockState withLit(boolean lit) {
            return this.state.withTrait(BlockTraits.IS_LIT, lit);
        }
    }

    private record PickleData(BlockState state) implements SeaPickle {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getPickles() {
            return this.state.ensureTrait(BlockTraits.CLUSTER_COUNT) + 1;
        }

        @Override
        public int getMinimumPickles() {
            return BlockTraits.CLUSTER_COUNT.getRange().getStart() + 1;
        }

        @Override
        public int getMaximumPickles() {
            return BlockTraits.CLUSTER_COUNT.getRange().getEnd() + 1;
        }

        @Override
        public BlockState withPickles(int pickles) {
            return withCount(this.state, BlockTraits.CLUSTER_COUNT, pickles);
        }
    }

    private record TurtleEggData(BlockState state) implements TurtleEgg {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getEggs() {
            return EGG_COUNTS.indexOf(this.state.ensureTrait(BlockTraits.TURTLE_EGG_COUNT)) + 1;
        }

        @Override
        public int getMinimumEggs() {
            return 1;
        }

        @Override
        public int getMaximumEggs() {
            return EGG_COUNTS.size();
        }

        @Override
        public BlockState withEggs(int eggs) {
            checkArgument(eggs >= this.getMinimumEggs() && eggs <= this.getMaximumEggs(), "Unsupported turtle egg count: %s", eggs);
            return this.state.withTrait(BlockTraits.TURTLE_EGG_COUNT, EGG_COUNTS.get(eggs - 1));
        }

        @Override
        public int getHatch() {
            return hatchStage(this.state);
        }

        @Override
        public int getMaximumHatch() {
            return HATCH_STAGES.size() - 1;
        }

        @Override
        public BlockState withHatch(int hatch) {
            return CloudBlockPropertyData.withHatch(this.state, hatch);
        }
    }

    private record SnowData(BlockState state) implements Snow {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getLayers() {
            return this.state.ensureTrait(BlockTraits.HEIGHT) + 1;
        }

        @Override
        public int getMinimumLayers() {
            return BlockTraits.HEIGHT.getRange().getStart() + 1;
        }

        @Override
        public int getMaximumLayers() {
            return BlockTraits.HEIGHT.getRange().getEnd() + 1;
        }

        @Override
        public BlockState withLayers(int layers) {
            return withCount(this.state, BlockTraits.HEIGHT, layers);
        }
    }

    private record SpeleothemData(BlockState state) implements Speleothem {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public Direction getVerticalDirection() {
            return this.state.ensureTrait(BlockTraits.IS_HANGING) ? Direction.DOWN : Direction.UP;
        }

        @Override
        public Set<Direction> getVerticalDirections() {
            return VERTICAL_DIRECTIONS;
        }

        @Override
        public BlockState withVerticalDirection(Direction direction) {
            requireNonNull(direction, "direction");
            checkArgument(VERTICAL_DIRECTIONS.contains(direction), "Unsupported tip direction: %s", direction);
            return this.state.withTrait(BlockTraits.IS_HANGING, direction == Direction.DOWN);
        }

        @Override
        public DripstoneThickness getThickness() {
            return this.state.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS);
        }

        @Override
        public BlockState withThickness(DripstoneThickness thickness) {
            return this.state.withTrait(BlockTraits.DRIPSTONE_THICKNESS, requireNonNull(thickness, "thickness"));
        }
    }
}
