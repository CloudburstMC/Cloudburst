package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.CollisionShapeHandler;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;

public final class GetCollisionShapeHandler implements CollisionShapeHandler {

    @Override
    public VoxelShape execute(BlockState state, BlockShapeContext blockContext, CollisionContext collisionContext) {
        return state.getCollisionShape();
    }
}
