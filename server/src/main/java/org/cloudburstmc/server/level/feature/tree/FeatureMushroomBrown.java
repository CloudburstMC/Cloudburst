package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

import java.util.random.RandomGenerator;

/**
 * Generates a huge brown mushroom.
 *
 * @author DaPorkchop_
 */
public class FeatureMushroomBrown extends FeatureAbstractTree {
    public FeatureMushroomBrown(@NonNull IntRange height) {
        super(height);
    }

    @Override
    protected int chooseHeight(ChunkManager level, RandomGenerator random, int x, int y, int z) {
        return this.height.rand(random) << (~random.nextInt(12) & 1);
    }

    @Override
    protected boolean canPlace(ChunkManager level, RandomGenerator random, int x, int y, int z, int height) {
        for (int dy = 0; dy <= height + 1; dy++) {
            if (y + dy < 0 || y + dy >= 256) {
                return false;
            }

            int radius = dy < 3 ? 0 : 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!this.test(level.getBlockState(x, y + dy, z, 0))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected BlockState selectLog(ChunkManager level, RandomGenerator random, int x, int y, int z, int height) {
        return BlockStates.BROWN_MUSHROOM_BLOCK;//.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomBrown.STEM);
    }

    @Override
    protected BlockState selectLeaves(ChunkManager level, RandomGenerator random, int x, int y, int z, int height) {
        return null;
    }

    @Override
    protected void placeLeaves(ChunkManager level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        //as ugly as it is, this makes more sense to hardcode than trying to be smart about it
        int yy = y + height;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockState(x + dx, yy, z + dz, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlockState(x + dx, yy, z + 3, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_S));
            level.setBlockState(x + dx, yy, z - 3, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_N));
        }
        for (int dz = -1; dz <= 1; dz++) {
            level.setBlockState(x + 3, yy, z + dz, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_E));
            level.setBlockState(x - 3, yy, z + dz, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_W));
        }
        level.setBlockState(x + 2, yy, z + 3, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_SE));
        level.setBlockState(x - 2, yy, z + 3, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_SW));
        level.setBlockState(x + 2, yy, z - 3, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_NE));
        level.setBlockState(x - 2, yy, z - 3, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_NW));
        level.setBlockState(x + 3, yy, z + 2, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_SE));
        level.setBlockState(x + 3, yy, z - 2, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_NE));
        level.setBlockState(x - 3, yy, z + 2, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_SW));
        level.setBlockState(x - 3, yy, z - 2, BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 0));//BlockBehaviorHugeMushroomBrown.TOP_NW));
    }

    @Override
    protected void placeTrunk(ChunkManager level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        for (int dy = 0; dy < height; dy++) {
            level.setBlockState(x, y + dy, z, 0, log);
        }
    }

    @Override
    protected void finish(ChunkManager level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        this.replaceGrassWithDirt(level, x, y - 1, z);
    }
}
