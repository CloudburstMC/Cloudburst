package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;

import static org.cloudburstmc.server.block.BlockTypes.FLOWING_WATER;
import static org.cloudburstmc.server.block.BlockTypes.WATER;

@RequiredArgsConstructor
public class CloudBlock implements Block {

    private final BlockState state;
    private final Level level;
    private final Chunk chunk;
    private final Vector3i position;
    private final int layer;

    @Override
    public BlockState getState(int layer) {
        return state;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public Vector3i getPosition() {
        return position;
    }

    @Override
    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public Block getSide(Direction face, int step) {
        return this.level.getBlock(face.getOffset(this.position, step));
    }

    @Override
    public Block getRelative(int x, int y, int z) {
        return this.level.getBlock(this.position.add(x, y, z));
    }

    @Override
    public boolean isWaterlogged() {
        BlockState fluidState = this.getExtra();
        return (fluidState.getType() == WATER || fluidState.getType() == FLOWING_WATER);
    }

    @Override
    public int getLayer() {
        return layer;
    }

    @Override
    public void set(BlockState state, int layer, boolean direct, boolean update) {
        this.level.setBlock(this.position, layer, state, direct, update);
    }

    @Override
    public BlockSnapshot snapshot() {
        return new CloudBlockSnapshot(new BlockState[]{this.getState(0), this.getState(1)});
    }
}
