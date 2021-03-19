package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.block.behavior.BlockBehaviorHugeMushroomRed;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

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
        level.setBlockState(x, yy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP));
        level.setBlockState(x + 1, yy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_E));
        level.setBlockState(x + 1, yy, z + 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_SE));
        level.setBlockState(x, yy, z + 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_S));
        level.setBlockState(x - 1, yy, z + 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_SW));
        level.setBlockState(x - 1, yy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_W));
        level.setBlockState(x - 1, yy, z - 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_NW));
        level.setBlockState(x, yy, z - 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_N));
        level.setBlockState(x + 1, yy, z - 1, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.TOP_NE));
    }

    protected void placeSideColumn(ChunkManager level, int x, int y, int z, int damage) {
        for (int dy = 0; dy < 3; dy++) {
            level.setBlockState(x, y + dy, z, BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, damage));
        }
    }
}
