package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.data.BlockData;
import org.cloudburstmc.api.block.data.MultipleFacing;
import org.cloudburstmc.api.block.data.RedstoneWire;
import org.cloudburstmc.api.block.data.Wall;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.RedstoneSide;
import org.cloudburstmc.api.util.data.WallConnectionType;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Typed views of independent faces, wall heights and redstone connections.
 */
@UtilityClass
public class CloudBlockConnections {

    public static final Map<Direction, BooleanBlockTrait> BOOLEAN_FACES = Map.of(
            Direction.DOWN, BlockTraits.CONNECTION_DOWN,
            Direction.UP, BlockTraits.CONNECTION_UP,
            Direction.NORTH, BlockTraits.CONNECTION_NORTH,
            Direction.SOUTH, BlockTraits.CONNECTION_SOUTH,
            Direction.WEST, BlockTraits.CONNECTION_WEST,
            Direction.EAST, BlockTraits.CONNECTION_EAST);

    public static final Map<Direction, Integer> MULTIFACE_BITS = Map.of(
            Direction.DOWN, 1, Direction.UP, 2, Direction.SOUTH, 4,
            Direction.WEST, 8, Direction.NORTH, 16, Direction.EAST, 32);

    public static final Map<Direction, Integer> VINE_BITS = Map.of(
            Direction.SOUTH, 1, Direction.WEST, 2, Direction.NORTH, 4, Direction.EAST, 8);

    public static final Map<Direction, EnumBlockTrait<WallConnectionType>> WALL_FACES = Map.of(
            Direction.NORTH, BlockTraits.WALL_CONNECTION_NORTH,
            Direction.SOUTH, BlockTraits.WALL_CONNECTION_SOUTH,
            Direction.WEST, BlockTraits.WALL_CONNECTION_WEST,
            Direction.EAST, BlockTraits.WALL_CONNECTION_EAST);

    public static final Map<Direction, EnumBlockTrait<RedstoneSide>> WIRE_FACES = Map.of(
            Direction.NORTH, BlockTraits.REDSTONE_NORTH,
            Direction.SOUTH, BlockTraits.REDSTONE_SOUTH,
            Direction.WEST, BlockTraits.REDSTONE_WEST,
            Direction.EAST, BlockTraits.REDSTONE_EAST);

    /**
     * @param state    the state to view
     * @param dataType the requested face or connection interface
     * @param <D>      the view type
     * @return the supported view, or empty when unavailable
     */
    public static <D extends BlockData> Optional<D> get(BlockState state, Class<D> dataType) {
        @Nullable BlockData data = null;
        if (dataType == MultipleFacing.class) {
            data = multipleFacing(state);
        } else if (dataType == Wall.class && state.getTraits().containsKey(BlockTraits.HAS_POST)
                && state.getTraits().keySet().containsAll(WALL_FACES.values())) {
            data = new WallData(state);
        } else if (dataType == RedstoneWire.class && state.getTraits().containsKey(BlockTraits.REDSTONE_SIGNAL)
                && state.getTraits().keySet().containsAll(WIRE_FACES.values())) {
            data = new WireData(state);
        }

        return Optional.ofNullable(dataType.cast(data));
    }

    private static @Nullable MultipleFacing multipleFacing(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.MULTI_FACE_DIRECTION)) {
            return new BitFacesData(state, BlockTraits.MULTI_FACE_DIRECTION, MULTIFACE_BITS);
        }

        if (state.getTraits().containsKey(BlockTraits.VINE_DIRECTION_BITS)) {
            return new BitFacesData(state, BlockTraits.VINE_DIRECTION_BITS, VINE_BITS);
        }

        Map<Direction, BooleanBlockTrait> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, BooleanBlockTrait> face : BOOLEAN_FACES.entrySet()) {
            if (state.getTraits().containsKey(face.getValue())) {
                faces.put(face.getKey(), face.getValue());
            }
        }

        return faces.isEmpty() ? null : new BooleanFacesData(state, Map.copyOf(faces));
    }

    private static <T> T faceValue(Map<Direction, T> values, Direction face) {
        T value = values.get(requireNonNull(face, "face"));
        checkArgument(value != null, "Unsupported face: %s", face);
        return value;
    }

    private record BooleanFacesData(BlockState state, Map<Direction, BooleanBlockTrait> faces) implements MultipleFacing {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean hasFace(Direction face) {
            return this.state.ensureTrait(faceValue(this.faces, face));
        }

        @Override
        public Set<Direction> getFaces() {
            Set<Direction> enabled = EnumSet.noneOf(Direction.class);
            for (Direction face : this.faces.keySet()) {
                if (this.hasFace(face)) {
                    enabled.add(face);
                }
            }

            return Set.copyOf(enabled);
        }

        @Override
        public Set<Direction> getAllowedFaces() {
            return this.faces.keySet();
        }

        @Override
        public BlockState withFace(Direction face, boolean enabled) {
            return this.state.withTrait(faceValue(this.faces, face), enabled);
        }
    }

    private record BitFacesData(BlockState state, IntegerBlockTrait trait, Map<Direction, Integer> faces) implements MultipleFacing {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean hasFace(Direction face) {
            return (this.state.ensureTrait(this.trait) & faceValue(this.faces, face)) != 0;
        }

        @Override
        public Set<Direction> getFaces() {
            Set<Direction> enabled = EnumSet.noneOf(Direction.class);
            for (Direction face : this.faces.keySet()) {
                if (this.hasFace(face)) {
                    enabled.add(face);
                }
            }

            return Set.copyOf(enabled);
        }

        @Override
        public Set<Direction> getAllowedFaces() {
            return this.faces.keySet();
        }

        @Override
        public BlockState withFace(Direction face, boolean enabled) {
            int bit = faceValue(this.faces, face);
            int current = this.state.ensureTrait(this.trait);
            return this.state.withTrait(this.trait, enabled ? current | bit : current & ~bit);
        }
    }

    private record WallData(BlockState state) implements Wall {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isUp() {
            return this.state.ensureTrait(BlockTraits.HAS_POST);
        }

        @Override
        public BlockState withUp(boolean up) {
            return this.state.withTrait(BlockTraits.HAS_POST, up);
        }

        @Override
        public WallConnectionType getHeight(Direction face) {
            return this.state.ensureTrait(faceValue(WALL_FACES, face));
        }

        @Override
        public BlockState withHeight(Direction face, WallConnectionType height) {
            return this.state.withTrait(faceValue(WALL_FACES, face), height);
        }
    }

    private record WireData(BlockState state) implements RedstoneWire {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getPower() {
            return this.state.ensureTrait(BlockTraits.REDSTONE_SIGNAL);
        }

        @Override
        public int getMaximumPower() {
            return BlockTraits.REDSTONE_SIGNAL.getRange().getEnd();
        }

        @Override
        public BlockState withPower(int power) {
            return this.state.withTrait(BlockTraits.REDSTONE_SIGNAL, power);
        }

        @Override
        public RedstoneSide getFace(Direction face) {
            return this.state.ensureTrait(faceValue(WIRE_FACES, face));
        }

        @Override
        public BlockState withFace(Direction face, RedstoneSide connection) {
            return this.state.withTrait(faceValue(WIRE_FACES, face), connection);
        }
    }
}
