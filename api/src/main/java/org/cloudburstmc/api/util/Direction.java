package org.cloudburstmc.api.util;

import com.google.common.collect.Iterators;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public enum Direction {
    DOWN(1, -1, "down", AxisDirection.NEGATIVE, Vector3i.from(0, -1, 0)),
    UP(0, -1, "up", AxisDirection.POSITIVE, Vector3i.from(0, 1, 0)),
    NORTH(3, 2, "north", AxisDirection.NEGATIVE, Vector3i.from(0, 0, -1)),
    SOUTH(2, 0, "south", AxisDirection.POSITIVE, Vector3i.from(0, 0, 1)),
    WEST(5, 1, "west", AxisDirection.NEGATIVE, Vector3i.from(-1, 0, 0)),
    EAST(4, 3, "east", AxisDirection.POSITIVE, Vector3i.from(1, 0, 0));

    /**
     * All faces in D-U-N-S-W-E order.
     */
    private static final Direction[] VALUES = new Direction[6];

    /**
     * Horizontal faces in S-W-N-E order.
     */
    private static final Direction[] HORIZONTALS = new Direction[4];

    static {
        // Axis and CardinalDirection cannot be set in the enum constructor
        // because they are themselves enums referencing Direction constants.
        DOWN.axis = Axis.Y;
        UP.axis = Axis.Y;
        NORTH.axis = Axis.Z;
        SOUTH.axis = Axis.Z;
        WEST.axis = Axis.X;
        EAST.axis = Axis.X;

        NORTH.cardinalDirection = CardinalDirection.NORTH;
        SOUTH.cardinalDirection = CardinalDirection.SOUTH;
        WEST.cardinalDirection = CardinalDirection.WEST;
        EAST.cardinalDirection = CardinalDirection.EAST;

        for (Direction face : values()) {
            VALUES[face.ordinal()] = face;
            if (face.horizontalIndex >= 0) {
                HORIZONTALS[face.horizontalIndex] = face;
            }
        }
    }

    private final int opposite;
    private final int horizontalIndex;
    private final String name;
    private final AxisDirection axisDirection;
    private final Vector3i unitVector;
    private Axis axis;
    private CardinalDirection cardinalDirection;

    Direction(int opposite, int horizontalIndex, String name, AxisDirection axisDirection, Vector3i unitVector) {
        this.opposite = opposite;
        this.horizontalIndex = horizontalIndex;
        this.name = name;
        this.axisDirection = axisDirection;
        this.unitVector = unitVector;
    }

    /**
     * Returns the face with the given 3-D index (0–5, D-U-N-S-W-E order).
     */
    public static Direction fromIndex(int index) {
        return VALUES[Math.abs(index % VALUES.length)];
    }

    /**
     * Returns the horizontal face with the given index (0–3, S-W-N-E order).
     */
    public static Direction fromHorizontalIndex(int index) {
        return HORIZONTALS[Math.abs(index % HORIZONTALS.length)];
    }

    /**
     * Returns the horizontal face closest to the given yaw angle (degrees).
     * 0° → SOUTH, 90° → WEST, 180° → NORTH, 270° → EAST.
     */
    public static Direction fromYaw(double yaw) {
        return fromHorizontalIndex(GenericMath.floor(yaw / 90.0D + 0.5D) & 3);
    }

    /**
     * Returns the face for the given axis and axis-direction combination.
     */
    public static Direction of(Axis axis, AxisDirection axisDirection) {
        for (Direction face : VALUES) {
            if (face.getAxis() == axis && face.getAxisDirection() == axisDirection) {
                return face;
            }
        }
        throw new IllegalArgumentException("No face for axis=" + axis + " direction=" + axisDirection);
    }

    /**
     * Returns a random face.
     */
    public static Direction random(Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }

    /**
     * Returns the 3-D index of this face (0–5, D-U-N-S-W-E order).
     */
    public int getIndex() {
        return ordinal();
    }

    /**
     * Returns the horizontal index of this face (0–3, S-W-N-E order).
     * Returns {@code -1} for vertical faces (UP/DOWN).
     */
    public int getHorizontalIndex() {
        return horizontalIndex;
    }

    /**
     * Returns the X component of this direction's unit vector (-1, 0, or +1).
     */
    public int getStepX() {
        return axis == Axis.X ? axisDirection.getStep() : 0;
    }

    /**
     * Returns the Y component of this direction's unit vector (-1, 0, or +1).
     */
    public int getStepY() {
        return axis == Axis.Y ? axisDirection.getStep() : 0;
    }

    /**
     * Returns the Z component of this direction's unit vector (-1, 0, or +1).
     */
    public int getStepZ() {
        return axis == Axis.Z ? axisDirection.getStep() : 0;
    }

    /**
     * Rotates this face clockwise around the Y axis.
     * NORTH → EAST → SOUTH → WEST → NORTH.
     *
     * @throws IllegalStateException for vertical faces
     */
    public Direction rotateClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalStateException("Cannot rotate vertical face " + this + " around Y");
        };
    }

    /**
     * Rotates this face counter-clockwise around the Y axis.
     * NORTH → WEST → SOUTH → EAST → NORTH.
     *
     * @throws IllegalStateException for vertical faces
     */
    public Direction rotateCounterClockwise() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
            default -> throw new IllegalStateException("Cannot rotate vertical face " + this + " around Y");
        };
    }

    /**
     * Rotates this face clockwise around the given axis.
     * Faces that are parallel to the axis (aligned with it) are returned unchanged.
     */
    public Direction rotateClockwise(Axis axis) {
        return switch (axis) {
            case X -> (this == WEST || this == EAST) ? this : rotateClockwiseX();
            case Y -> (this == UP || this == DOWN) ? this : rotateClockwise();
            case Z -> (this == NORTH || this == SOUTH) ? this : rotateClockwiseZ();
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    /**
     * Rotates this face counter-clockwise around the given axis.
     * Faces that are parallel to the axis are returned unchanged.
     */
    public Direction rotateCounterClockwise(Axis axis) {
        return switch (axis) {
            case X -> (this == WEST || this == EAST) ? this : rotateCounterClockwiseX();
            case Y -> (this == UP || this == DOWN) ? this : rotateCounterClockwise();
            case Z -> (this == NORTH || this == SOUTH) ? this : rotateCounterClockwiseZ();
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private Direction rotateClockwiseX() {
        return switch (this) {
            case DOWN -> SOUTH;
            case UP -> NORTH;
            case NORTH -> DOWN;
            case SOUTH -> UP;
            default -> throw new IllegalStateException("Cannot X-rotate face " + this);
        };
    }

    private Direction rotateCounterClockwiseX() {
        return switch (this) {
            case DOWN -> NORTH;
            case UP -> SOUTH;
            case NORTH -> UP;
            case SOUTH -> DOWN;
            default -> throw new IllegalStateException("Cannot X-rotate face " + this);
        };
    }

    private Direction rotateClockwiseZ() {
        return switch (this) {
            case DOWN -> WEST;
            case UP -> EAST;
            case WEST -> UP;
            case EAST -> DOWN;
            default -> throw new IllegalStateException("Cannot Z-rotate face " + this);
        };
    }

    private Direction rotateCounterClockwiseZ() {
        return switch (this) {
            case DOWN -> EAST;
            case UP -> WEST;
            case WEST -> DOWN;
            case EAST -> UP;
            default -> throw new IllegalStateException("Cannot Z-rotate face " + this);
        };
    }

    /**
     * Returns {@code true} if this direction is generally facing the given yaw angle (degrees).
     */
    public boolean isFacing(float yaw) {
        float rad = yaw * (float) (Math.PI / 180.0);
        float sinYaw = -(float) Math.sin(rad);
        float cosYaw = (float) Math.cos(rad);
        return unitVector.getX() * sinYaw + unitVector.getZ() * cosYaw > 0.0F;
    }

    /**
     * Returns the face directly opposite this one (e.g. DOWN → UP).
     */
    public Direction getOpposite() {
        return fromIndex(opposite);
    }

    public CardinalDirection getCardinalDirection() {
        return cardinalDirection;
    }

    /**
     * Returns the yaw angle (degrees, 0–270 in 90° increments) for this horizontal face.
     */
    public float toYaw() {
        return (float) ((horizontalIndex & 3) * 90);
    }

    public String getName() {
        return name;
    }

    public Axis getAxis() {
        return axis;
    }

    public AxisDirection getAxisDirection() {
        return axisDirection;
    }

    public Vector3i getUnitVector() {
        return unitVector;
    }

    public Vector3i relative(Vector3i pos) {
        return pos.add(unitVector);
    }

    public Vector3i relative(Vector3i pos, int distance) {
        return pos.add(unitVector.getX() * distance, unitVector.getY() * distance, unitVector.getZ() * distance);
    }

    @Override
    public String toString() {
        return name;
    }

    public enum Axis implements Predicate<Direction> {
        X("x") {
            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }

            @Override
            public Direction positive() {
                return Direction.EAST;
            }

            @Override
            public Direction negative() {
                return Direction.WEST;
            }
        },
        Y("y") {
            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }

            @Override
            public Direction positive() {
                return Direction.UP;
            }

            @Override
            public Direction negative() {
                return Direction.DOWN;
            }
        },
        Z("z") {
            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }

            @Override
            public Direction positive() {
                return Direction.SOUTH;
            }

            @Override
            public Direction negative() {
                return Direction.NORTH;
            }
        };

        static {
            X.plane = Plane.HORIZONTAL;
            Y.plane = Plane.VERTICAL;
            Z.plane = Plane.HORIZONTAL;
        }

        private final String name;
        private Plane plane;

        Axis(String name) {
            this.name = name;
        }

        /**
         * Selects the value corresponding to this axis from the three components.
         */
        public abstract int choose(int x, int y, int z);

        /**
         * Selects the value corresponding to this axis from the three components.
         */
        public abstract double choose(double x, double y, double z);

        /**
         * Returns the positive-direction {@link Direction} along this axis.
         */
        public abstract Direction positive();

        /**
         * Returns the negative-direction {@link Direction} along this axis.
         */
        public abstract Direction negative();

        public boolean isVertical() {
            return plane == Plane.VERTICAL;
        }

        public boolean isHorizontal() {
            return plane == Plane.HORIZONTAL;
        }

        public Plane getPlane() {
            return plane;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean test(Direction face) {
            return face != null && face.getAxis() == this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String description;

        AxisDirection(int step, String description) {
            this.step = step;
            this.description = description;
        }

        /**
         * Returns +1 for {@link #POSITIVE}, -1 for {@link #NEGATIVE}.
         */
        public int getStep() {
            return step;
        }

        /**
         * Returns the opposite axis direction.
         */
        public AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public enum Plane implements Predicate<Direction>, Iterable<Direction> {
        HORIZONTAL(new Direction[]{NORTH, EAST, SOUTH, WEST}),
        VERTICAL(new Direction[]{UP, DOWN});

        private final Direction[] faces;

        Plane(Direction[] faces) {
            this.faces = faces;
        }

        public Direction random() {
            return this.faces[ThreadLocalRandom.current().nextInt(this.faces.length)];
        }

        public Direction random(Random rand) {
            return this.faces[rand.nextInt(this.faces.length)];
        }

        @Override
        public boolean test(Direction face) {
            return face != null && face.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<Direction> iterator() {
            return Iterators.forArray(faces);
        }
    }
}
