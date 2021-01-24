package org.cloudburstmc.server.world.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.behavior.BlockBehaviorHugeMushroomRed;
import org.cloudburstmc.server.world.ChunkManager;
import org.cloudburstmc.server.world.generator.standard.misc.IntRange;

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
    protected BlockState selectLog(ChunkManager level, PRandom random, int x, int y, int z, int height) {
        return BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.STEM);
    }

    @Override
    protected void placeLeaves(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
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
        level.setBlockAt(x, yy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP));
        level.setBlockAt(x + 1, yy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_E));
        level.setBlockAt(x + 1, yy, z + 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_SE));
        level.setBlockAt(x, yy, z + 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_S));
        level.setBlockAt(x - 1, yy, z + 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_SW));
        level.setBlockAt(x - 1, yy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_W));
        level.setBlockAt(x - 1, yy, z - 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_NW));
        level.setBlockAt(x, yy, z - 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_N));
        level.setBlockAt(x + 1, yy, z - 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_NE));
    }

    protected void placeSideColumn(ChunkManager level, int x, int y, int z, int damage) {
        for (int dy = 0; dy < 3; dy++) {
            level.setBlockAt(x, y + dy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, damage));
        }
    }
}
