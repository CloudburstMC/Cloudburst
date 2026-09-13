package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.CollisionShapeHandler;
import org.cloudburstmc.api.block.component.EntityInsideBlockHandler;
import org.cloudburstmc.api.block.component.VoxelShapeBlockHandler;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

@UtilityClass
public class EndGatewayBlockHandlers {

    public static final CollisionShapeHandler COLLISION_SHAPE = (state, blockContext, collisionContext) -> CloudVoxelShapes.empty();

    public static final VoxelShapeBlockHandler ENTITY_INSIDE_SHAPE = (state, context) -> CloudVoxelShapes.block();

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (entity instanceof CloudEntity cloudEntity) {
            cloudEntity.enterEndGateway(block.getPosition());
        }
    };
}
