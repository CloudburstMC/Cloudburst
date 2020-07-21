package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import lombok.Value;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.registry.BlockRegistry;

import static org.cloudburstmc.server.block.BlockTypes.FLOWING_WATER;
import static org.cloudburstmc.server.block.BlockTypes.WATER;

@Value
public class CloudBlock implements Block {
    private final BlockState state;
    private final Level level;
    private final Chunk chunk;
    private final Vector3i position;
    private final int layer;

    @Override
    public BlockBehavior getBehaviour() {
        return BlockRegistry.get().getBehavior(state.getType());
    }

    @Override
    public Block getSide(BlockFace face, int step) {
        return this.getLevel().getBlock(face.getOffset(this.position, step));
    }

    @Override
    public Block getExtra() {
        return this.level.getBlock(this.position, 1);
    }

    @Override
    public boolean isWaterlogged() {
        BlockState fluidState = this.level.getBlockAt(this.position, 1);
        return (fluidState.getType() == WATER || fluidState.getType() == FLOWING_WATER);
    }

    @Override
    public void set(BlockState state, boolean direct, boolean update) {
        this.level.setBlock(this.position, this.layer, state, direct, update);
    }
}
