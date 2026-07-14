package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.VoxelShapeBlockHandler;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;

public final class GetOutlineShapeHandler implements VoxelShapeBlockHandler {

    @Override
    public VoxelShape execute(BlockState state, CollisionContext context) {
        return state.getOutlineShape();
    }
}
