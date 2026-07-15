package org.cloudburstmc.server.entity.vehicle;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class DismountHelper {

    public static @Nullable Vector3f findSafeDismountLocation(CloudLevel level, Vector3i position, boolean avoidDanger) {
        Block feetBlock = level.getBlock(position);
        Block headBlock = level.getBlock(position.add(0, 1, 0));
        Block floorBlock = level.getBlock(position.sub(0, 1, 0));

        if (!isPassable(level, feetBlock) || !isPassable(level, headBlock) || isPassable(level, floorBlock)) {
            return null;
        }
        if (avoidDanger && (isDangerous(feetBlock) || isDangerous(floorBlock))) {
            return null;
        }

        return Vector3f.from(position.getX() + 0.5f, position.getY(), position.getZ() + 0.5f);
    }

    private static boolean isPassable(CloudLevel level, Block block) {
        return !level.hasBlockCollision(null, block.getState(), block.getPosition(), BoundingBox.unit(block.getPosition()));
    }

    private static boolean isDangerous(Block block) {
        BlockType type = block.getState().getType();
        return type == BlockTypes.FIRE
                || type == BlockTypes.SOUL_FIRE
                || type == BlockTypes.LAVA
                || type == BlockTypes.FLOWING_LAVA
                || type == BlockTypes.CACTUS
                || type == BlockTypes.MAGMA
                || type == BlockTypes.WITHER_ROSE;
    }
}
