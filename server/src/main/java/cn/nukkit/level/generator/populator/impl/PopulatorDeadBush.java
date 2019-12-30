package cn.nukkit.level.generator.populator.impl;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockIds;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.chunk.Chunk;
import cn.nukkit.level.generator.populator.helper.EnsureBelow;
import cn.nukkit.level.generator.populator.helper.EnsureCover;
import cn.nukkit.level.generator.populator.type.PopulatorSurfaceBlock;
import cn.nukkit.math.NukkitRandom;

import static cn.nukkit.block.BlockIds.SAND;

/**
 * @author DaPorkchop_
 */
public class PopulatorDeadBush extends PopulatorSurfaceBlock {
    private static final Block DEAD_BUSH = Block.get(BlockIds.DEADBUSH);

    @Override
    protected boolean canStay(int x, int y, int z, Chunk chunk, ChunkManager level) {
        return EnsureCover.ensureCover(x, y, z, chunk) && EnsureBelow.ensureBelow(x, y, z, SAND, chunk);
    }

    @Override
    protected Block getBlock(int x, int z, NukkitRandom random, Chunk chunk) {
        return DEAD_BUSH;
    }
}
