package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.MovementType;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

@UtilityClass
public class BlockShapeTransitions {

    public static void pushEntitiesUp(CloudLevel level, Vector3i position, BlockState from, BlockState to) {
        VoxelShape fromShape = from.getCollisionShape();
        VoxelShape toShape = to.getCollisionShape();
        VoxelShape addedShape = CloudVoxelShapes.onlySecond(fromShape, toShape).move(position.getX(), position.getY(), position.getZ());
        if (addedShape.isEmpty()) {
            return;
        }

        BoundingBox affectedArea = addedShape.bounds();
        for (Entity entity : level.getNearbyEntities(affectedArea)) {
            if (!(entity instanceof CloudEntity cloudEntity)) {
                continue;
            }

            BoundingBox raisedEntity = entity.getBoundingBox().move(0, 1, 0);
            float movement = 1 + addedShape.collide(Direction.Axis.Y, raisedEntity, -1);
            if (movement > 0) {
                cloudEntity.move(MovementType.PISTON, 0, movement, 0);
            }
        }
    }
}
