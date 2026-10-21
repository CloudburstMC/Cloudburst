package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.BlockSupportShapeHandler;
import org.cloudburstmc.api.block.component.CollisionShapeHandler;
import org.cloudburstmc.api.block.component.ShapeContextRequirement;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.block.BlockGeometryData;
import org.cloudburstmc.server.block.BlockModelOffset;
import org.cloudburstmc.server.block.BlockPalette;

public class ModelOffsetShapeHandler implements CollisionShapeHandler, BlockSupportShapeHandler {

    private final BlockPalette palette;
    private final boolean outline;

    public ModelOffsetShapeHandler(BlockPalette palette, boolean outline) {
        this.palette = palette;
        this.outline = outline;
    }

    @Override
    public VoxelShape execute(BlockState state, BlockShapeContext blockContext, CollisionContext collisionContext) {
        return this.execute(state, blockContext);
    }

    @Override
    public VoxelShape execute(BlockState state, BlockShapeContext context) {
        VoxelShape shape = this.outline ? state.getOutlineShape() : state.getCollisionShape();
        BlockGeometryData geometry = this.palette.getGeometry(state);
        if (context.isEmpty() || geometry == null) {
            return shape;
        }

        BlockModelOffset offset = this.outline ? geometry.outlineOffset() : geometry.collisionOffset();
        return offset == null ? shape : offset.apply(shape, context.position());
    }

    @Override
    public ShapeContextRequirement contextRequirement() {
        return ShapeContextRequirement.POSITION;
    }
}
