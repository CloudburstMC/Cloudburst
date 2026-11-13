package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.*;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Typed views of block layout, halves and mounting surfaces.
 */
@UtilityClass
public class CloudBlockStructureData {

    /**
     * @param state the state to view
     * @param dataType the requested layout or attachment interface
     * @param <D> the view type
     * @return the supported view, or empty when unavailable
     */
    public static <D extends BlockData> Optional<D> get(BlockState state, Class<D> dataType) {
        @Nullable BlockData data = null;
        if (dataType == Slab.class && state.getType() instanceof CloudBlockType type) {
            SlabFamily family = type.getSlabFamily();
            if (family != null) {
                data = new SlabData(state, family);
            }
        } else if (dataType == Stairs.class && state.getTraits().containsKey(BlockTraits.STAIR_SHAPE)
                && state.getTraits().containsKey(BlockTraits.IS_UPSIDE_DOWN)
                && state.getData(Directional.class).isPresent()) {
            data = new StairData(state);
        } else if (dataType == Rail.class) {
            if (state.getTraits().containsKey(BlockTraits.RAIL_DIRECTION)) {
                data = new RailData(state, BlockTraits.RAIL_DIRECTION);
            } else if (state.getTraits().containsKey(BlockTraits.SIMPLE_RAIL_DIRECTION)) {
                data = new RailData(state, BlockTraits.SIMPLE_RAIL_DIRECTION);
            }
        } else if (dataType == Scaffolding.class && state.getType() == BlockTypes.SCAFFOLDING) {
            data = new ScaffoldingData(state);
        } else if (dataType == Bisected.class) {
            data = bisected(state);
        } else if (dataType == FaceAttachable.class) {
            if (state.getTraits().containsKey(BlockTraits.LEVER_DIRECTION)) {
                data = new LeverAttachmentData(state);
            } else if (state.getTraits().containsKey(BlockTraits.ATTACHMENT)) {
                data = new AttachmentData(state);
            } else if (state.getTraits().containsKey(BlockTraits.IS_BUTTON_PRESSED) && state.getTraits().containsKey(BlockTraits.FACING_DIRECTION)) {
                data = new ButtonAttachmentData(state);
            }
        }

        return Optional.ofNullable(dataType.cast(data));
    }

    private static @Nullable Bisected bisected(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.IS_UPPER_BLOCK)) {
            return new HalfData(state, BlockTraits.IS_UPPER_BLOCK);
        }

        if (state.getTraits().containsKey(BlockTraits.IS_UPSIDE_DOWN)) {
            return new HalfData(state, BlockTraits.IS_UPSIDE_DOWN);
        }

        if (state.getTraits().containsKey(BlockTraits.SEA_GRASS_TYPE) && state.ensureTrait(BlockTraits.SEA_GRASS_TYPE) != SeaGrassType.DEFAULT) {
            return new SeagrassHalfData(state);
        }

        return null;
    }

    private record ScaffoldingData(BlockState state) implements Scaffolding {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getDistance() {
            return this.state.ensureTrait(BlockTraits.STABILITY);
        }

        @Override
        public int getMaximumDistance() {
            return BlockTraits.STABILITY.getRange().getEnd();
        }

        @Override
        public BlockState withDistance(int distance) {
            return this.state.withTrait(BlockTraits.STABILITY, distance);
        }
    }

    private record SlabData(BlockState state, SlabFamily family) implements Slab {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public SlabType getType() {
            if (this.state.getType() == this.family.doubled()) {
                return SlabType.DOUBLE;
            }

            return this.state.ensureTrait(BlockTraits.SLAB_SLOT) == SlabSlot.TOP ? SlabType.TOP : SlabType.BOTTOM;
        }

        @Override
        public BlockState withType(SlabType type) {
            requireNonNull(type, "type");
            if (this.getType() == type) {
                return this.state;
            }

            BlockState target = (type == SlabType.DOUBLE ? this.family.doubled() : this.family.single()).getDefaultState().copyTraits(this.state);
            return target.withTrait(BlockTraits.SLAB_SLOT, type == SlabType.TOP ? SlabSlot.TOP : SlabSlot.BOTTOM);
        }
    }

    private record HalfData(BlockState state, BooleanBlockTrait upper) implements Bisected {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public BlockHalf getHalf() {
            return this.state.ensureTrait(this.upper) ? BlockHalf.TOP : BlockHalf.BOTTOM;
        }

        @Override
        public BlockState withHalf(BlockHalf half) {
            return this.state.withTrait(this.upper, requireNonNull(half, "half") == BlockHalf.TOP);
        }
    }

    private record SeagrassHalfData(BlockState state) implements Bisected {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public BlockHalf getHalf() {
            return this.state.ensureTrait(BlockTraits.SEA_GRASS_TYPE) == SeaGrassType.DOUBLE_TOP ? BlockHalf.TOP : BlockHalf.BOTTOM;
        }

        @Override
        public BlockState withHalf(BlockHalf half) {
            SeaGrassType value = requireNonNull(half, "half") == BlockHalf.TOP ? SeaGrassType.DOUBLE_TOP : SeaGrassType.DOUBLE_BOTTOM;
            return this.state.withTrait(BlockTraits.SEA_GRASS_TYPE, value);
        }
    }

    private record StairData(BlockState state) implements Stairs {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public Direction getFacing() {
            return this.directional().getFacing();
        }

        @Override
        public Set<Direction> getFaces() {
            return this.directional().getFaces();
        }

        @Override
        public BlockState withFacing(Direction facing) {
            return this.directional().withFacing(facing);
        }

        @Override
        public BlockHalf getHalf() {
            return this.state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN) ? BlockHalf.TOP : BlockHalf.BOTTOM;
        }

        @Override
        public BlockState withHalf(BlockHalf half) {
            return this.state.withTrait(BlockTraits.IS_UPSIDE_DOWN, requireNonNull(half, "half") == BlockHalf.TOP);
        }

        @Override
        public StairShape getShape() {
            return this.state.ensureTrait(BlockTraits.STAIR_SHAPE);
        }

        @Override
        public BlockState withShape(StairShape shape) {
            return this.state.withTrait(BlockTraits.STAIR_SHAPE, shape);
        }

        private Directional directional() {
            return this.state.getData(Directional.class).orElseThrow();
        }
    }

    private record RailData(BlockState state, EnumBlockTrait<RailDirection> trait) implements Rail {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public RailDirection getShape() {
            return this.state.ensureTrait(this.trait);
        }

        @Override
        public Set<RailDirection> getShapes() {
            return Set.copyOf(this.trait.getPossibleValues());
        }

        @Override
        public BlockState withShape(RailDirection shape) {
            return this.state.withTrait(this.trait, shape);
        }
    }

    private record ButtonAttachmentData(BlockState state) implements FaceAttachable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public AttachedFace getAttachedFace() {
            return switch (this.state.ensureTrait(BlockTraits.FACING_DIRECTION)) {
                case UP -> AttachedFace.FLOOR;
                case DOWN -> AttachedFace.CEILING;
                default -> AttachedFace.WALL;
            };
        }

        @Override
        public BlockState withAttachedFace(AttachedFace face) {
            if (this.getAttachedFace() == requireNonNull(face, "face")) {
                return this.state;
            }

            Direction direction = switch (face) {
                case FLOOR -> Direction.UP;
                case CEILING -> Direction.DOWN;
                case WALL -> Direction.NORTH;
            };

            return this.state.withTrait(BlockTraits.FACING_DIRECTION, direction);
        }
    }

    private record LeverAttachmentData(BlockState state) implements FaceAttachable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public AttachedFace getAttachedFace() {
            return switch (this.state.ensureTrait(BlockTraits.LEVER_DIRECTION).getDirection()) {
                case UP -> AttachedFace.FLOOR;
                case DOWN -> AttachedFace.CEILING;
                default -> AttachedFace.WALL;
            };
        }

        @Override
        public BlockState withAttachedFace(AttachedFace face) {
            if (this.getAttachedFace() == requireNonNull(face, "face")) {
                return this.state;
            }

            LeverDirection current = this.state.ensureTrait(BlockTraits.LEVER_DIRECTION);
            Direction alignment = switch (current) {
                case UP_EAST_WEST, DOWN_EAST_WEST -> Direction.EAST;
                case UP_NORTH_SOUTH, DOWN_NORTH_SOUTH -> Direction.NORTH;
                default -> current.getDirection();
            };

            Direction direction = switch (face) {
                case FLOOR -> Direction.UP;
                case CEILING -> Direction.DOWN;
                case WALL -> alignment;
            };

            return this.state.withTrait(BlockTraits.LEVER_DIRECTION, LeverDirection.forDirection(direction, alignment));
        }
    }

    private record AttachmentData(BlockState state) implements FaceAttachable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public AttachedFace getAttachedFace() {
            return switch (this.state.ensureTrait(BlockTraits.ATTACHMENT)) {
                case STANDING -> AttachedFace.FLOOR;
                case HANGING -> AttachedFace.CEILING;
                case SIDE, MULTIPLE -> AttachedFace.WALL;
            };
        }

        @Override
        public BlockState withAttachedFace(AttachedFace face) {
            if (this.getAttachedFace() == requireNonNull(face, "face")) {
                return this.state;
            }

            AttachmentType attachment = switch (face) {
                case FLOOR -> AttachmentType.STANDING;
                case WALL -> AttachmentType.SIDE;
                case CEILING -> AttachmentType.HANGING;
            };

            return this.state.withTrait(BlockTraits.ATTACHMENT, attachment);
        }
    }
}
