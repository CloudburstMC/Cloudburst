package org.cloudburstmc.server.level.feature;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

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
    public boolean place(ChunkManager level, PRandom random, int x, int y, int z) {
        if (y <= 0 || y >= 255) {
            return false;
        }

        final int size = this.size.rand(random);
        final Direction direction = Direction.Plane.HORIZONTAL.random(random);
        for (int i = 0; i < size; i++) {
            if (!this.test(level.getBlockState(x + direction.getXOffset() * i, y, z + direction.getZOffset() * i, 0))
                    || this.testOrLiquid(level.getBlockState(x + direction.getXOffset() * i, y - 1, z + direction.getZOffset() * i, 0))) {
                return false;
            }
        }

        level.setBlockAt(x, y, z, 0, this.log);

        BlockState log = this.log.withTrait(BlockTraits.AXIS, direction.getAxis());
        for (int i = random.nextInt(2) + 2; i < size; i++) {
            level.setBlockAt(x + direction.getXOffset() * i, y, z + direction.getZOffset() * i, 0, log);

            if (random.nextInt(10) == 0 && this.test(level.getBlockState(x + direction.getXOffset() * i, y + 1, z + direction.getZOffset() * i, 0))) {
                level.setBlockAt(x + direction.getXOffset() * i, y + 1, z + direction.getZOffset() * i, 0, random.nextBoolean() ? BlockStates.BROWN_MUSHROOM : BlockStates.RED_MUSHROOM);
            }

            this.replaceGrassWithDirt(level, x + direction.getXOffset() * i, y - 1, z + direction.getZOffset() * i);
        }

        if (this.vineChance > 0.0d) {
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x - 1, y, z, 0))) {
                level.setBlockAt(x - 1, y, z, 0, BlockStates.VINE.withTrait(BlockTraits.FACING_DIRECTION, Direction.EAST));
            }
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x + 1, y, z, 0))) {
                level.setBlockAt(x + 1, y, z, 0, BlockStates.VINE.withTrait(BlockTraits.FACING_DIRECTION, Direction.WEST));
            }
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x, y, z - 1, 0))) {
                level.setBlockAt(x, y, z - 1, 0, BlockStates.VINE.withTrait(BlockTraits.FACING_DIRECTION, Direction.SOUTH));
            }
            if (random.nextDouble() < this.vineChance && this.test(level.getBlockState(x, y, z + 1, 0))) {
                level.setBlockAt(x, y, z + 1, 0, BlockStates.VINE.withTrait(BlockTraits.FACING_DIRECTION, Direction.NORTH));
            }
        }

        return true;
    }
}
