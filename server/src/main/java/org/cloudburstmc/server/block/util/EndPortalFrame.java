package org.cloudburstmc.server.block.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

public record EndPortalFrame(Vector3i center) {

    private static final FrameBlock[] FRAME_BLOCKS = {
            new FrameBlock(-1, -2, Direction.SOUTH),
            new FrameBlock(0, -2, Direction.SOUTH),
            new FrameBlock(1, -2, Direction.SOUTH),
            new FrameBlock(-1, 2, Direction.NORTH),
            new FrameBlock(0, 2, Direction.NORTH),
            new FrameBlock(1, 2, Direction.NORTH),
            new FrameBlock(-2, -1, Direction.EAST),
            new FrameBlock(-2, 0, Direction.EAST),
            new FrameBlock(-2, 1, Direction.EAST),
            new FrameBlock(2, -1, Direction.WEST),
            new FrameBlock(2, 0, Direction.WEST),
            new FrameBlock(2, 1, Direction.WEST)
    };

    public static @Nullable EndPortalFrame find(CloudLevel level, Vector3i framePosition) {
        BlockState frame = level.getBlockState(framePosition);
        if (isInvalidFrameBlock(frame)) {
            return null;
        }

        Direction facing = frame.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
        for (Vector3i center : possibleCenters(framePosition, facing)) {
            if (isComplete(level, center)) {
                return new EndPortalFrame(center);
            }
        }

        return null;
    }

    public void fill(CloudLevel level) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Vector3i position = this.center.add(x, 0, z);
                level.setBlockState(position, BlockStates.END_PORTAL);
            }
        }
    }

    public void clear(CloudLevel level) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Vector3i position = this.center.add(x, 0, z);
                if (level.getBlockState(position).getType() == BlockTypes.END_PORTAL) {
                    level.setBlockState(position, BlockStates.AIR);
                }
            }
        }
    }

    private static boolean isComplete(CloudLevel level, Vector3i center) {
        for (FrameBlock frame : FRAME_BLOCKS) {
            BlockState state = level.getBlockState(center.add(frame.x(), 0, frame.z()));
            if (isInvalidFrameBlock(state) || state.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection() != frame.facing()) {
                return false;
            }
        }

        return true;
    }

    private static boolean isInvalidFrameBlock(BlockState state) {
        return state.getType() != BlockTypes.END_PORTAL_FRAME
                || !state.ensureTrait(BlockTraits.HAS_END_PORTAL_EYE);
    }

    private static Vector3i[] possibleCenters(Vector3i frame, Direction facing) {
        int x = frame.getX();
        int y = frame.getY();
        int z = frame.getZ();
        return switch (facing) {
            case NORTH -> new Vector3i[]{
                    Vector3i.from(x - 1, y, z - 2),
                    Vector3i.from(x, y, z - 2),
                    Vector3i.from(x + 1, y, z - 2)
            };
            case SOUTH -> new Vector3i[]{
                    Vector3i.from(x - 1, y, z + 2),
                    Vector3i.from(x, y, z + 2),
                    Vector3i.from(x + 1, y, z + 2)
            };
            case WEST -> new Vector3i[]{
                    Vector3i.from(x - 2, y, z - 1),
                    Vector3i.from(x - 2, y, z),
                    Vector3i.from(x - 2, y, z + 1)
            };
            case EAST -> new Vector3i[]{
                    Vector3i.from(x + 2, y, z - 1),
                    Vector3i.from(x + 2, y, z),
                    Vector3i.from(x + 2, y, z + 1)
            };
            default -> throw new IllegalArgumentException("End portal frame must face horizontally: " + facing);
        };
    }

    private record FrameBlock(int x, int z, Direction facing) {
    }
}
