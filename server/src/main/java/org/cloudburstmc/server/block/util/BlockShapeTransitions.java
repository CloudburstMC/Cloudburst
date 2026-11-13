package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.function.Predicate;

@UtilityClass
public class BlockShapeTransitions {

    public static void pushEntitiesUp(Level level, Vector3i position, BlockState from, BlockState to) {
        pushEntitiesUp(level, position, from, to, entity -> true);
    }

    public static void pushEntitiesUp(Level level, Vector3i position, BlockState from, BlockState to, Predicate<? super CloudEntity> filter) {
        VoxelShape fromShape = from.getCollisionShape();
        VoxelShape toShape = to.getCollisionShape();
        VoxelShape addedShape = CloudVoxelShapes.onlySecond(fromShape, toShape).move(position.getX(), position.getY(), position.getZ());
        if (addedShape.isEmpty()) {
            return;
        }

        BoundingBox affectedArea = addedShape.bounds();
        for (Entity entity : level.getNearbyEntities(affectedArea)) {
            if (!(entity instanceof CloudEntity cloudEntity) || !filter.test(cloudEntity)) {
                continue;
            }

            float movement = getUpwardDisplacement(addedShape, entity.getBoundingBox());
            if (movement > 0) {
                cloudEntity.displace(Vector3f.from(0, movement, 0));
            }
        }
    }

    private static float getUpwardDisplacement(VoxelShape addedShape, BoundingBox entityBounds) {
        if (!addedShape.overlaps(entityBounds)) {
            return 0;
        }

        BoundingBox raisedEntity = entityBounds.move(0, 1, 0);
        return 1 + addedShape.collide(Direction.Axis.Y, raisedEntity, -1);
    }
}
