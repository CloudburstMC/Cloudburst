package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.BlockShapeHandler;
import org.cloudburstmc.api.util.VoxelShape;

public final class GetOutlineShapeHandler implements BlockShapeHandler {

    @Override
    public VoxelShape execute(BlockState state, BlockShapeContext context) {
        return state.getOutlineShape();
    }
}
