package org.cloudburstmc.api.util.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.Direction;

@RequiredArgsConstructor
@Getter
public enum LeverDirection {
    DOWN_EAST_WEST("down_east_west", Direction.DOWN),
    EAST("east", Direction.EAST),
    WEST("west", Direction.WEST),
    SOUTH("south", Direction.SOUTH),
    NORTH("north", Direction.NORTH),
    UP_NORTH_SOUTH("up_north_south", Direction.UP),
    UP_EAST_WEST("up_east_west", Direction.UP),
    DOWN_NORTH_SOUTH("down_north_south", Direction.DOWN);

    private final String name;
    private final Direction direction;

    public static LeverDirection forDirection(Direction clickedSide, Direction playerDirection) {
        switch (clickedSide) {
            case DOWN:
                switch (playerDirection.getAxis()) {
                    case X:
                        return DOWN_EAST_WEST;
                    case Z:
                        return DOWN_NORTH_SOUTH;
                    default:
                        throw new IllegalArgumentException("Invalid entityFacing " + playerDirection + " for facing " + clickedSide);
                }
            case UP:
                switch (playerDirection.getAxis()) {
                    case X:
                        return UP_EAST_WEST;
                    case Z:
                        return UP_NORTH_SOUTH;
                    default:
                        throw new IllegalArgumentException("Invalid entityFacing " + playerDirection + " for facing " + clickedSide);
                }
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            case EAST:
                return EAST;
            default:
                throw new IllegalArgumentException("Invalid facing: " + clickedSide);
        }
    }
}
