package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.CollisionShapeHandler;
import org.cloudburstmc.api.block.component.EntityInsideBlockHandler;
import org.cloudburstmc.api.block.component.PlayerBlockHandler;
import org.cloudburstmc.api.block.component.VoxelShapeBlockHandler;
import org.cloudburstmc.server.block.util.EndPortalFrame;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

@UtilityClass
public class EndPortalBlockHandlers {

    public static final CollisionShapeHandler COLLISION_SHAPE = (state, blockContext, collisionContext) -> CloudVoxelShapes.empty();

    public static final VoxelShapeBlockHandler ENTITY_INSIDE_SHAPE = (state, context) -> CloudVoxelShapes.box(0, 0.375f, 0, 1, 0.75f, 1);

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (entity instanceof CloudEntity cloudEntity) {
            cloudEntity.enterEndPortal(block.getPosition());
        }
    };

    public static final PlayerBlockHandler DESTROY_FRAME = (block, player) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        EndPortalFrame frame = EndPortalFrame.find(level, block.getPosition());
        if (frame != null) {
            frame.clear(level);
        }

        block.set(BlockTypes.AIR.getDefaultState(), true, true);
    };
}
