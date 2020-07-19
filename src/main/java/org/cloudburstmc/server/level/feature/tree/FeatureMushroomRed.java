package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.behavior.BlockBehaviorHugeMushroomRed;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * Generates a huge red mushroom.
 *
 * @author DaPorkchop_
 */
public class FeatureMushroomRed extends FeatureMushroomBrown {
    public FeatureMushroomRed(@NonNull IntRange height) {
        super(height);
    }

    @Override
    protected int selectLog(ChunkManager level, PRandom random, int x, int y, int z, int height) {
        return BlockRegistry.get().getRuntimeId(BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.STEM);
    }

    @Override
    protected void placeLeaves(ChunkManager level, PRandom random, int x, int y, int z, int height, int log, int leaves) {
        //as ugly as it is, this makes more sense to hardcode than trying to be smart about it
        int yy = y + height - 3;
        this.placeSideColumn(level, x + 2, yy, z, BlockBehaviorHugeMushroomRed.TOP_E);
        this.placeSideColumn(level, x + 2, yy, z + 1, BlockBehaviorHugeMushroomRed.TOP_SE);
        this.placeSideColumn(level, x + 2, yy, z - 1, BlockBehaviorHugeMushroomRed.TOP_NE);
        this.placeSideColumn(level, x - 2, yy, z, BlockBehaviorHugeMushroomRed.TOP_W);
        this.placeSideColumn(level, x - 2, yy, z + 1, BlockBehaviorHugeMushroomRed.TOP_SW);
        this.placeSideColumn(level, x - 2, yy, z - 1, BlockBehaviorHugeMushroomRed.TOP_NW);
        this.placeSideColumn(level, x, yy, z + 2, BlockBehaviorHugeMushroomRed.TOP_S);
        this.placeSideColumn(level, x + 1, yy, z + 2, BlockBehaviorHugeMushroomRed.TOP_SE);
        this.placeSideColumn(level, x - 1, yy, z + 2, BlockBehaviorHugeMushroomRed.TOP_SW);
        this.placeSideColumn(level, x, yy, z - 2, BlockBehaviorHugeMushroomRed.TOP_N);
        this.placeSideColumn(level, x + 1, yy, z - 2, BlockBehaviorHugeMushroomRed.TOP_NE);
        this.placeSideColumn(level, x - 1, yy, z - 2, BlockBehaviorHugeMushroomRed.TOP_NW);

        yy = y + height;
        level.setBlockAt(x, yy, z, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP);
        level.setBlockAt(x + 1, yy, z, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_E);
        level.setBlockAt(x + 1, yy, z + 1, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_SE);
        level.setBlockAt(x, yy, z + 1, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_S);
        level.setBlockAt(x - 1, yy, z + 1, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_SW);
        level.setBlockAt(x - 1, yy, z, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_W);
        level.setBlockAt(x - 1, yy, z - 1, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_NW);
        level.setBlockAt(x, yy, z - 1, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_N);
        level.setBlockAt(x + 1, yy, z - 1, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.TOP_NE);
    }

    protected void placeSideColumn(ChunkManager level, int x, int y, int z, int damage) {
        for (int dy = 0; dy < 3; dy++) {
            level.setBlockAt(x, y + dy, z, BlockTypes.RED_MUSHROOM_BLOCK, damage);
        }
    }
}
