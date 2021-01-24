package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;

import static org.cloudburstmc.server.block.BlockIds.FLOWING_WATER;
import static org.cloudburstmc.server.block.BlockIds.WATER;

public class CloudBlock extends CloudBlockSnapshot implements Block {

    public static BlockState[] EMPTY = new BlockState[]{BlockStates.AIR, BlockStates.AIR};

    private final World world;
    private final Vector3i position;

    public CloudBlock(World world, Vector3i position, BlockState[] states) {
        super(states);
        this.world = world;
        this.position = position;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public Vector3i getPosition() {
        return position;
    }

    @Override
    public Chunk getChunk() {
        return world.getChunk(position);
    }

    @Override
    public Block getSide(Direction face, int step) {
        return this.world.getBlock(face.getOffset(this.position, step));
    }

    @Override
    public BlockState getRelativeState(int x, int y, int z, int layer) {
        return this.world.getBlockAt(getX() + x, getY() + y, getZ() + z, layer);
    }

    @Override
    public BlockState getSideState(Direction face, int step, int layer) {
        return this.world.getBlockAt(
                getX() + face.getXOffset() * step,
                getY() + face.getYOffset() * step,
                getZ() + face.getZOffset() * step,
                layer
        );
    }

    @Override
    public Block getRelative(int x, int y, int z) {
        return this.world.getBlock(this.position.add(x, y, z));
    }

    @Override
    public boolean isWaterlogged() {
        BlockState fluidState = this.getExtra();
        return (fluidState.getType() == WATER || fluidState.getType() == FLOWING_WATER);
    }

    @Override
    public void set(BlockState state, int layer, boolean direct, boolean update) {
        this.world.setBlock(this.position, layer, state, direct, update);
    }

    @Override
    public BlockSnapshot snapshot() {
        return new CloudBlockSnapshot(new BlockState[]{this.getState(0), this.getState(1)});
    }

    @Override
    public Block refresh() {
        return world.getBlock(this.position);
    }
}
