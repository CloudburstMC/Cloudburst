package org.cloudburstmc.api.util;

import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@RequiredArgsConstructor
public enum Direction {
    DOWN(1, -1, "down", AxisDirection.NEGATIVE, null, Vector3i.from(0, -1, 0)),
    UP(0, -1, "up", AxisDirection.POSITIVE, null, Vector3i.from(0, 1, 0)),
    NORTH(3, 2, "north", AxisDirection.NEGATIVE, CardinalDirection.NORTH, Vector3i.from(0, 0, -1)),
    SOUTH(2, 0, "south", AxisDirection.POSITIVE, CardinalDirection.SOUTH, Vector3i.from(0, 0, 1)),
    WEST(5, 1, "west", AxisDirection.NEGATIVE, CardinalDirection.WEST, Vector3i.from(-1, 0, 0)),
    EAST(4, 3, "east", AxisDirection.POSITIVE, CardinalDirection.EAST, Vector3i.from(1, 0, 0));

    /**
     * All faces in D-U-N-S-W-E order
     */
    private static final Direction[] VALUES = new Direction[6];

    /**
     * All faces with horizontal axis in order S-W-N-E
     */
    private static final Direction[] HORIZONTALS = new Direction[4];

    static {
        //Circular dependency
        DOWN.axis = Axis.Y;
        UP.axis = Axis.Y;
        NORTH.axis = Axis.Z;
        SOUTH.axis = Axis.Z;
        WEST.axis = Axis.X;
        EAST.axis = Axis.X;

        for (Direction face : values()) {
            VALUES[face.ordinal()] = face;

            if (face.getAxis().isHorizontal()) {
                HORIZONTALS[face.horizontalIndex] = face;
            }
        }
    }

    /**
     * Index of the opposite BlockFace in the VALUES array
     */
    private final int opposite;

    /**
     * Ordering index for the HORIZONTALS field (S-W-N-E)
     */
    private final int horizontalIndex;

    /**
     * The name of this BlockFace (up, down, north, etc.)
     */
    private final String name;


    private Axis axis;
    private final AxisDirection axisDirection;

    private final CardinalDirection cardinalDirection;

    /**
     * Normalized vector that points in the direction of this BlockFace
     */
    private final Vector3i unitVector;

    /**
     * Get a BlockFace by it's index (0-5). The order is D-U-N-S-W-E
     *
     * @param index BlockFace index
     * @return block face
     */
    public static Direction fromIndex(int index) {
        return VALUES[Math.abs(index % VALUES.length)];
    }

    /**
     * Get a BlockFace by it's horizontal index (0-3). The order is S-W-N-E
     *
     * @param index BlockFace index
     * @return block face
     */
    public static Direction fromHorizontalIndex(int index) {
        return HORIZONTALS[Math.abs(index % HORIZONTALS.length)];
    }

    /**
     * Get the BlockFace corresponding to the given angle (0-360). An angle of 0 is SOUTH, an angle of 90 would be WEST
     *
     * @param angle horizontal angle
     * @return block face
     */
    public static Direction fromHorizontalAngle(double angle) {
        return fromHorizontalIndex(GenericMath.floor(angle / 90.0D + 0.5D) & 3);
    }

    public static Direction fromAxis(AxisDirection axisDirection, Axis axis) {
        for (Direction face : VALUES) {
            if (face.getAxisDirection() == axisDirection && face.getAxis() == axis) {
                return face;
            }
        }

        throw new RuntimeException("Unable to get face from axis: " + axisDirection + " " + axis);
    }

    /**
     * Choose a random BlockFace using the given Random
     *
     * @param rand random number generator
     * @return block face
     */
    public static Direction random(Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }

    /**
     * Get the index of this BlockFace (0-5). The order is D-U-N-S-W-E
     *
     * @return index
     */
    public int getIndex() {
        return ordinal();
    }

    /**
     * Get the horizontal index of this BlockFace (0-3). The order is S-W-N-E
     *
     * @return horizontal index
     */
    public int getHorizontalIndex() {
        return horizontalIndex;
    }

    public CardinalDirection getCardinalDirection() {
        return cardinalDirection;
    }

    /**
     * Get the angle of this BlockFace (0-360)
     *
     * @return horizontal angle
     */
    public float getHorizontalAngle() {
        return (float) ((horizontalIndex & 3) * 90);
    }

    /**
     * Get the name of this BlockFace (up, down, north, etc.)
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Axis of this BlockFace
     *
     * @return axis
     */
    public Axis getAxis() {
        return axis;
    }

    /**
     * Get the AxisDirection of this BlockFace
     *
     * @return axis direction
     */
    public AxisDirection getAxisDirection() {
        return axisDirection;
    }

    /**
     * Get the unit vector of this BlockFace
     *
     * @return vector
     */
    public Vector3i getUnitVector() {
        return unitVector;
    }

    public Vector3i getOffset(Vector3i pos) {
        return pos.add(unitVector);
    }

    public Vector3i getOffset(Vector3i pos, int step) {
        return pos.add(unitVector.getX() * step, unitVector.getY() * step, unitVector.getZ() * step);
    }

    /**
     * Returns an offset that addresses the block in front of this BlockFace
     *
     * @return x offset
     */
    public int getXOffset() {
        return axis == Axis.X ? axisDirection.getOffset() : 0;
    }

    /**
     * Returns an offset that addresses the block in front of this BlockFace
     *
     * @return y offset
     */
    public int getYOffset() {
        return axis == Axis.Y ? axisDirection.getOffset() : 0;
    }

    /**
     * Returns an offset that addresses the block in front of this BlockFace
     *
     * @return x offset
     */
    public int getZOffset() {
        return axis == Axis.Z ? axisDirection.getOffset() : 0;
    }

    /**
     * Get the opposite BlockFace (e.g. DOWN ==&gt; UP)
     *
     * @return block face
     */
    public Direction getOpposite() {
        return fromIndex(opposite);
    }

    /**
     * Rotate this BlockFace around the Y axis clockwise (NORTH =&gt; EAST =&gt; SOUTH =&gt; WEST =&gt; BB_NORTH)
     *
     * @return block face
     */
    public Direction rotateY() {
        switch (this) {
            case NORTH:
                return EAST;
            case EAST:
                return SOUTH;
            case SOUTH:
                return WEST;
            case WEST:
                return NORTH;
            default:
                throw new RuntimeException("Unable to get Y-rotated face of " + this);
        }
    }

    /**
     * Rotate this BlockFace around the Y axis counter-clockwise (NORTH =&gt; WEST =&gt; SOUTH =&gt; EAST =&gt; BB_NORTH)
     *
     * @return block face
     */
    public Direction rotateYCCW() {
        switch (this) {
            case NORTH:
                return WEST;
            case EAST:
                return NORTH;
            case SOUTH:
                return EAST;
            case WEST:
                return SOUTH;
            default:
                throw new RuntimeException("Unable to get counter-clockwise Y-rotated face of " + this);
        }
    }

    public String toString() {
        return name;
    }

    public enum Axis implements Predicate<Direction> {
        X("x"),
        Y("y"),
        Z("z");

        private final String name;
        private Plane plane;

        static {
            //Circular dependency
            X.plane = Plane.HORIZONTAL;
            Y.plane = Plane.VERTICAL;
            Z.plane = Plane.HORIZONTAL;
        }

        Axis(String name) {
            this.name = name;
        }

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

        public boolean test(Direction face) {
            return face != null && face.getAxis() == this;
        }

        public String toString() {
            return name;
        }
    }

    public enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int offset;
        private final String description;

        AxisDirection(int offset, String description) {
            this.offset = offset;
            this.description = description;
        }

        public int getOffset() {
            return offset;
        }

        public String toString() {
            return description;
        }
    }

    public enum Plane implements Predicate<Direction>, Iterable<Direction> {
        HORIZONTAL,
        VERTICAL;

        static {
            //Circular dependency
            HORIZONTAL.faces = new Direction[]{NORTH, EAST, SOUTH, WEST};
            VERTICAL.faces = new Direction[]{UP, DOWN};
        }

        private Direction[] faces;

        public Direction random() {
            return this.faces[ThreadLocalRandom.current().nextInt(this.faces.length)];
        }

        public Direction random(Random rand) {
            return this.faces[rand.nextInt(this.faces.length)];
        }

        public boolean test(Direction face) {
            return face != null && face.getAxis().getPlane() == this;
        }

        @Nonnull
        public Iterator<Direction> iterator() {
            return Iterators.forArray(faces);
        }
    }
}
