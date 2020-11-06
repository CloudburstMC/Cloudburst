package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import lombok.ToString;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;

import static org.cloudburstmc.server.block.BlockTypes.FLOWING_WATER;
import static org.cloudburstmc.server.block.BlockTypes.WATER;

@ToString(exclude = {"level"}, callSuper = true)
public class CloudBlock extends CloudBlockSnapshot implements Block {

    public static BlockState[] EMPTY = new BlockState[]{BlockStates.AIR, BlockStates.AIR};

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
    public Vector3i getPosition() {
        return position;
    }

    @Override
    public Chunk getChunk() {
        return level.getChunk(position);
    }

    @Override
    public Block getSide(Direction face, int step) {
        return this.level.getBlock(face.getOffset(this.position, step));
    }

    @Override
    public BlockState getRelativeState(int x, int y, int z, int layer) {
        return this.level.getBlockAt(getX() + x, getY() + y, getZ() + z, layer);
    }

    @Override
    public BlockState getSideState(Direction face, int step, int layer) {
        return this.level.getBlockAt(
                getX() + face.getXOffset() * step,
                getY() + face.getYOffset() * step,
                getZ() + face.getZOffset() * step,
                layer
        );
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
    public void set(BlockState state, int layer, boolean direct, boolean update) {
        this.level.setBlock(this.position, layer, state, direct, update);
    }

    @Override
    public BlockSnapshot snapshot() {
        return new CloudBlockSnapshot(new BlockState[]{this.getState(0), this.getState(1)});
    }

    @Override
    public Block refresh() {
        return level.getBlock(this.position);
    }
}
