package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

@UtilityClass
public class PlacementSupport {

    public static boolean hasFloorSupport(Level level, Vector3i pos) {
        return hasFaceSupport(level, pos, Direction.UP, SupportType.FULL);
    }

    public static boolean hasCenterFaceSupport(Level level, Vector3i pos, Direction face) {
        return hasFaceSupport(level, pos, face, SupportType.CENTER);
    }

    public static boolean hasFullFaceSupport(Level level, Vector3i pos, Direction face) {
        return hasFaceSupport(level, pos, face, SupportType.FULL);
    }

    public static boolean hasFaceSupport(Level level, Vector3i pos, Direction face, SupportType supportType) {
        Vector3i supportPos = supportPosition(pos, face);
        BlockState support = level.getBlockState(supportPos.getX(), supportPos.getY(), supportPos.getZ());
        return support != BlockStates.AIR && BlockSupport.isFaceSturdy(level, supportPos, face, supportType);
    }

    public static Vector3i supportPosition(Vector3i pos, Direction face) {
        return face.getOpposite().relative(pos);
    }
}
