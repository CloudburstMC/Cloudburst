package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.*;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

/**
 * Horizontal transformations of canonical block states and connection properties.
 */
@UtilityClass
public class CloudBlockTransforms {

    private static final CardinalDirection[] ROTATIONS = CardinalDirection.values();

    private static final Map<Direction, EnumBlockTrait<WallConnectionType>> MOSS_FACES = Map.of(
            Direction.NORTH, BlockTraits.PALE_MOSS_CARPET_SIDE_NORTH,
            Direction.EAST, BlockTraits.PALE_MOSS_CARPET_SIDE_EAST,
            Direction.SOUTH, BlockTraits.PALE_MOSS_CARPET_SIDE_SOUTH,
            Direction.WEST, BlockTraits.PALE_MOSS_CARPET_SIDE_WEST
    );

    private static final Map<CrafterOrientation, Orientation> CRAFTER_ORIENTATIONS = Map.ofEntries(
            entry(CrafterOrientation.DOWN_EAST, new Orientation(Direction.DOWN, Direction.EAST)),
            entry(CrafterOrientation.DOWN_NORTH, new Orientation(Direction.DOWN, Direction.NORTH)),
            entry(CrafterOrientation.DOWN_SOUTH, new Orientation(Direction.DOWN, Direction.SOUTH)),
            entry(CrafterOrientation.DOWN_WEST, new Orientation(Direction.DOWN, Direction.WEST)),
            entry(CrafterOrientation.UP_EAST, new Orientation(Direction.UP, Direction.EAST)),
            entry(CrafterOrientation.UP_NORTH, new Orientation(Direction.UP, Direction.NORTH)),
            entry(CrafterOrientation.UP_SOUTH, new Orientation(Direction.UP, Direction.SOUTH)),
            entry(CrafterOrientation.UP_WEST, new Orientation(Direction.UP, Direction.WEST)),
            entry(CrafterOrientation.WEST_UP, new Orientation(Direction.WEST, Direction.UP)),
            entry(CrafterOrientation.EAST_UP, new Orientation(Direction.EAST, Direction.UP)),
            entry(CrafterOrientation.NORTH_UP, new Orientation(Direction.NORTH, Direction.UP)),
            entry(CrafterOrientation.SOUTH_UP, new Orientation(Direction.SOUTH, Direction.UP))
    );

    private static final Map<Orientation, CrafterOrientation> CRAFTER_VALUES = indexOrientations();

    /**
     * @param state    the state to rotate
     * @param rotation the horizontal rotation
     * @return the canonical transformed state
     */
    public static BlockState rotate(BlockState state, BlockRotation rotation) {
        Transform transform = switch (requireNonNull(rotation, "rotation")) {
            case NONE -> Transform.IDENTITY;
            case CLOCKWISE_90 -> Transform.CLOCKWISE;
            case CLOCKWISE_180 -> Transform.HALF_TURN;
            case COUNTERCLOCKWISE_90 -> Transform.COUNTERCLOCKWISE;
        };

        return apply(requireNonNull(state, "state"), transform);
    }

    /**
     * @param state  the state to reflect
     * @param mirror the vertical reflection plane
     * @return the canonical transformed state
     */
    public static BlockState mirror(BlockState state, BlockMirror mirror) {
        Transform transform = switch (requireNonNull(mirror, "mirror")) {
            case NONE -> Transform.IDENTITY;
            case LEFT_RIGHT -> Transform.INVERT_Z;
            case FRONT_BACK -> Transform.INVERT_X;
        };

        return apply(requireNonNull(state, "state"), transform);
    }

    private static BlockState apply(BlockState original, Transform transform) {
        if (transform == Transform.IDENTITY) {
            return original;
        }

        BlockState result = original;
        for (BlockTrait<?> trait : original.getTraits().keySet()) {
            result = scalar(original, result, trait, transform);
        }

        result = faces(original, result, CloudBlockConnections.BOOLEAN_FACES, transform);
        result = faces(original, result, CloudBlockConnections.WALL_FACES, transform);
        result = faces(original, result, CloudBlockConnections.WIRE_FACES, transform);
        return faces(original, result, MOSS_FACES, transform);
    }

    private static <T extends Comparable<T>> BlockState scalar(BlockState original, BlockState result, BlockTrait<T> trait, Transform transform) {
        T value = original.ensureTrait(trait);
        Comparable<?> mapped;

        if (trait == BlockTraits.JIGSAW_ROTATION || trait == BlockTraits.CHALKBOARD_DIRECTION || trait == BlockTraits.CHEMISTRY_TABLE_DIRECTION) {
            throw new UnsupportedOperationException("Unsupported encoded block orientation: " + trait.getName());
        } else if (trait == BlockTraits.MULTI_FACE_DIRECTION) {
            mapped = mask((Integer) value, CloudBlockConnections.MULTIFACE_BITS, transform);
        } else if (trait == BlockTraits.VINE_DIRECTION_BITS) {
            mapped = mask((Integer) value, CloudBlockConnections.VINE_BITS, transform);
        } else if (trait == BlockTraits.HUGE_MUSHROOM_BITS) {
            mapped = mushroom((Integer) value, transform);
        } else if (trait == BlockTraits.CORAL_FAN_DIRECTION && transform.swapsAxes()) {
            mapped = 1 - (Integer) value;
        } else if (trait == BlockTraits.IS_DOOR_HINGE && transform.reflected()) {
            mapped = !(Boolean) value;
        } else if (trait == BlockTraits.POWERED_SHELF_TYPE && transform.reflected()) {
            int part = (Integer) value;
            mapped = part == 1 || part == 3 ? 4 - part : part;
        } else {
            mapped = orientedValue(value, transform);
        }

        return value.equals(mapped) ? result : result.withTrait(trait, trait.getValueClass().cast(mapped));
    }

    private static <T extends Comparable<T>> BlockState faces(BlockState original, BlockState result, Map<Direction, ? extends BlockTrait<T>> properties, Transform transform) {
        for (Map.Entry<Direction, ? extends BlockTrait<T>> face : properties.entrySet()) {
            if (!original.getTraits().containsKey(face.getValue())) {
                continue;
            }

            BlockTrait<T> target = properties.get(transform.direction(face.getKey()));
            checkArgument(target != null && original.getTraits().containsKey(target),
                    "Block type %s cannot represent transformed connection %s", original.getType(), face.getKey());
            result = result.withTrait(target, original.ensureTrait(face.getValue()));
        }

        return result;
    }

    private static int mask(int value, Map<Direction, Integer> bits, Transform transform) {
        int result = 0;
        for (Map.Entry<Direction, Integer> face : bits.entrySet()) {
            if ((value & face.getValue()) != 0) {
                result |= requireNonNull(bits.get(transform.direction(face.getKey())), "Transformed face bit");
            }
        }

        return result;
    }

    private static int mushroom(int value, Transform transform) {
        if (value < 1 || value > 9) {
            return value;
        }

        int x = (value - 1) % 3 - 1;
        int z = (value - 1) / 3 - 1;
        return (transform.z(x, z) + 1) * 3 + transform.x(x, z) + 2;
    }

    private static Comparable<?> orientedValue(Comparable<?> value, Transform transform) {
        return switch (value) {
            case Direction direction -> transform.direction(direction);
            case Direction.Axis axis -> axis(axis, transform);
            case CardinalDirection rotation -> ROTATIONS[transform.segment(rotation.ordinal())];
            case LeverDirection lever -> lever(lever, transform);
            case CrafterOrientation orientation -> crafter(orientation, transform);
            case RailDirection rail -> rail(rail, transform);
            case StairShape shape -> transform.reflected() ? reflectedStair(shape) : shape;
            default -> value;
        };
    }

    private static Direction.Axis axis(Direction.Axis axis, Transform transform) {
        if (!transform.swapsAxes() || axis == Direction.Axis.Y) {
            return axis;
        }

        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static LeverDirection lever(LeverDirection lever, Transform transform) {
        Direction alignment = switch (lever) {
            case UP_EAST_WEST, DOWN_EAST_WEST -> Direction.EAST;
            case UP_NORTH_SOUTH, DOWN_NORTH_SOUTH -> Direction.NORTH;
            default -> lever.getDirection();
        };

        return LeverDirection.forDirection(transform.direction(lever.getDirection()), transform.direction(alignment));
    }

    private static CrafterOrientation crafter(CrafterOrientation value, Transform transform) {
        Orientation original = requireNonNull(CRAFTER_ORIENTATIONS.get(value), "Crafter orientation");
        Orientation mapped = new Orientation(transform.direction(original.front()), transform.direction(original.top()));
        return requireNonNull(CRAFTER_VALUES.get(mapped), "Transformed crafter orientation");
    }

    private static RailDirection rail(RailDirection value, Transform transform) {
        Direction ascending = value.ascendingDirection();
        if (ascending != null) {
            return RailDirection.ascending(transform.direction(ascending));
        }

        Direction first = transform.direction(value.connectingDirections().getFirst());
        if (value.isStraight()) {
            return RailDirection.straight(first);
        }

        Direction second = transform.direction(value.connectingDirections().get(1));
        return RailDirection.curved(first, second);
    }

    private static StairShape reflectedStair(StairShape shape) {
        return switch (shape) {
            case STRAIGHT -> StairShape.STRAIGHT;
            case INNER_LEFT -> StairShape.INNER_RIGHT;
            case INNER_RIGHT -> StairShape.INNER_LEFT;
            case OUTER_LEFT -> StairShape.OUTER_RIGHT;
            case OUTER_RIGHT -> StairShape.OUTER_LEFT;
        };
    }

    private static Map<Orientation, CrafterOrientation> indexOrientations() {
        Map<Orientation, CrafterOrientation> values = new HashMap<>();
        CRAFTER_ORIENTATIONS.forEach((value, orientation) -> values.put(orientation, value));
        return Map.copyOf(values);
    }

    private record Orientation(Direction front, Direction top) {
    }

    private enum Transform {
        IDENTITY(1, 0, 0, 1, 0),
        CLOCKWISE(0, -1, 1, 0, 4),
        HALF_TURN(-1, 0, 0, -1, 8),
        COUNTERCLOCKWISE(0, 1, -1, 0, 12),
        INVERT_X(-1, 0, 0, 1, 0),
        INVERT_Z(1, 0, 0, -1, 8);

        private final int xx;
        private final int xz;
        private final int zx;
        private final int zz;
        private final int segmentOffset;

        Transform(int xx, int xz, int zx, int zz, int segmentOffset) {
            this.xx = xx;
            this.xz = xz;
            this.zx = zx;
            this.zz = zz;
            this.segmentOffset = segmentOffset;
        }

        private int x(int x, int z) {
            return this.xx * x + this.xz * z;
        }

        private int z(int x, int z) {
            return this.zx * x + this.zz * z;
        }

        private boolean reflected() {
            return this.xx * this.zz - this.xz * this.zx < 0;
        }

        private boolean swapsAxes() {
            return this.xx == 0;
        }

        private int segment(int segment) {
            return (this.segmentOffset + (this.reflected() ? -segment : segment)) & 15;
        }

        private Direction direction(Direction direction) {
            if (direction.getAxis() == Direction.Axis.Y) {
                return direction;
            }

            int x = this.x(direction.getStepX(), direction.getStepZ());
            int z = this.z(direction.getStepX(), direction.getStepZ());
            if (x != 0) {
                return x > 0 ? Direction.EAST : Direction.WEST;
            }

            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
