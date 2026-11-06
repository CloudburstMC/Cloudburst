package org.cloudburstmc.server.block;

import lombok.ToString;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;

import static org.cloudburstmc.api.block.BlockStates.AIR;

@ToString(exclude = {"level"}, callSuper = true)
public class CloudBlock extends CloudBlockSnapshot implements Block {

    public static BlockState[] EMPTY = new BlockState[]{AIR, AIR};

    private final Level level;
    private final Vector3i position;

    public CloudBlock(Level level, Vector3i position, BlockState[] states) {
        super(states);
        this.level = level;
        this.position = position;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public Chunk getChunk() {
        return level.getChunk(position);
    }

    @Override
    public Vector3i getPosition() {
        return position;
    }

    @Override
    public int getLightLevel() {
        return this.level.getFullLight(this.position);
    }

    @Override
    public int getSkyLight() {
        return this.getChunk().getSkyLight(this.getX() & 15, this.getY(), this.getZ() & 15);
    }

    @Override
    public int getBlockLight() {
        return this.getChunk().getBlockLight(this.getX() & 15, this.getY(), this.getZ() & 15);
    }

    @Override
    public ComponentMap getComponents() {
        BlockType type = this.getState().getType();
        return this.level.getServer().getBlockRegistry().requireComponents(type);
    }

    @Override
    public VoxelShape getCollisionShape(CollisionContext context) {
        return this.requireComponent(BlockComponents.GET_COLLISION_SHAPE)
                .execute(this.getState(), BlockShapeContext.at(this.level, this.position), context);
    }

    @Override
    public VoxelShape getOutlineShape() {
        return this.requireComponent(BlockComponents.GET_OUTLINE_SHAPE)
                .execute(this.getState(), BlockShapeContext.at(this.level, this.position));
    }

    @Override
    public VoxelShape getBlockSupportShape() {
        return BlockSupport.getBlockSupportShape(this.level, this.position);
    }

    @Override
    public boolean isFaceSturdy(Direction face, SupportType supportType) {
        return BlockSupport.isFaceSturdy(this.level, this.position, face, supportType);
    }

    @Override
    public Block getSide(Direction face, int step) {
        return this.level.getBlock(face.relative(this.position, step));
    }

    @Override
    public BlockState getRelativeState(int x, int y, int z, BlockLayer layer) {
        return this.level.getBlockState(getX() + x, getY() + y, getZ() + z, layer);
    }

    @Override
    public BlockState getSideState(Direction face, int step, BlockLayer layer) {
        return this.level.getBlockState(
                getX() + face.getStepX() * step,
                getY() + face.getStepY() * step,
                getZ() + face.getStepZ() * step,
                layer
        );
    }

    @Override
    public Block getRelative(int x, int y, int z) {
        return this.level.getBlock(this.position.add(x, y, z));
    }

    @Override
    public void set(BlockState state, BlockLayer layer, boolean direct, boolean update) {
        this.level.setBlockState(this.position, layer, state, direct, update);
    }

    @Override
    public BlockSnapshot snapshot() {
        return new CloudBlockSnapshot(this.getState(), this.getSecondaryState());
    }

    @Override
    public Block refresh() {
        return level.getBlock(this.position);
    }
}
