package org.cloudburstmc.server.level.feature;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FeatureFallenTree extends ReplacingWorldFeature {
    @NonNull
    protected final IntRange size;
    @NonNull
    protected final BlockState log;
    protected final double vineChance;

    @Override
    public boolean place(GenerationRegion level, RandomGenerator random, int x, int y, int z) {
        if (y <= 0 || y >= 255) {
            return false;
        }

        final int size = this.size.rand(random);
        final Direction direction = Direction.Plane.HORIZONTAL.random(random);
        for (int i = 0; i < size; i++) {
            if (!this.test(level.getBlockState(x + direction.getStepX() * i, y, z + direction.getStepZ() * i))
                    || this.testOrLiquid(level.getBlockState(x + direction.getStepX() * i, y - 1, z + direction.getStepZ() * i))) {
                return false;
            }
        }

        level.setBlockState(x, y, z, this.log);

        BlockState log = this.log.withTrait(BlockTraits.AXIS, direction.getAxis());
        for (int i = random.nextInt(2) + 2; i < size; i++) {
            level.setBlockState(x + direction.getStepX() * i, y, z + direction.getStepZ() * i, log);

            if (random.nextInt(10) == 0 && this.test(level.getBlockState(x + direction.getStepX() * i, y + 1, z + direction.getStepZ() * i))) {
                level.setBlockState(x + direction.getStepX() * i, y + 1, z + direction.getStepZ() * i, random.nextBoolean() ? BlockStates.BROWN_MUSHROOM : BlockStates.RED_MUSHROOM);
            }

            this.replaceGrassWithDirt(level, x + direction.getStepX() * i, y - 1, z + direction.getStepZ() * i);
        }

        if (this.vineChance > 0.0d) {
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x - 1, y, z))) {
                level.setBlockState(x - 1, y, z, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 8));
            }
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x + 1, y, z))) {
                level.setBlockState(x + 1, y, z, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 2));
            }
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x, y, z - 1))) {
                level.setBlockState(x, y, z - 1, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 1));
            }
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x, y, z + 1))) {
                level.setBlockState(x, y, z + 1, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 4));
            }
        }

        return true;
    }
}
