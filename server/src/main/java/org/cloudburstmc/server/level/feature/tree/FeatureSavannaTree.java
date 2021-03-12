package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.math.Direction;

import static java.lang.Math.abs;

/**
 * Generates an acacia (savanna) tree.
 *
 * @author DaPorkchop_
 */
public class FeatureSavannaTree extends FeatureNormalTree {
    public static final IntRange DEFAULT_HEIGHT = new IntRange(5, 9);

    public FeatureSavannaTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureSavannaTree(@NonNull IntRange height, @NonNull BlockSelector wood, @NonNull BlockSelector leaves) {
        super(height, wood, leaves);
    }

    @Override
    public boolean place(ChunkManager level, PRandom random, int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return false;
        }

        final int height = this.chooseHeight(level, random, x, y, z);

        if (!this.canPlace(level, random, x, y, z, height)) {
            return false;
        }

        final BlockState log = this.selectLog(level, random, x, y, z, height);
        final BlockState leaves = this.selectLeaves(level, random, x, y, z, height);

        int bendHeight = height - random.nextInt(4) - 1;
        int bendSize = 3 - random.nextInt(3);

        int dx = 0;
        int dz = 0;

        Direction direction = Direction.Plane.HORIZONTAL.random(random);

        for (int dy = 0; dy < height; dy++) {
            if (dy >= bendHeight && bendSize > 0) {
                dx += direction.getXOffset();
                dz += direction.getZOffset();
                bendSize--;
            }

            if (this.test(level.getBlockState(x + dx, y + dy, z + dz, 0))) {
                level.setBlockAt(x + dx, y + dy, z + dz, 0, log);
            }
        }
        this.placeLeaves(level, random, x + dx, y + height - 1, z + dz, height, log, leaves);

        Direction secondDirection = Direction.Plane.HORIZONTAL.random(random);
        if (direction == secondDirection) {
            return true;
        }

        int secondBendHeight = bendHeight - random.nextInt(2) - 1;
        int secondBendSize = 1 + random.nextInt(3);
        int lastPlacedY = 0;

        dx = dz = 0;
        for (; secondBendHeight < height && secondBendSize > 0; secondBendHeight++, secondBendSize--) {
            dx += secondDirection.getXOffset();
            dz += secondDirection.getZOffset();

            if (this.test(level.getBlockState(x + dx, y + secondBendHeight, z + dz, 0))) {
                level.setBlockAt(x + dx, y + secondBendHeight, z + dz, 0, log);
                lastPlacedY = y + secondBendHeight;
            }
        }
        if (lastPlacedY > 0) {
            this.placeLeaves(level, random, x + dx, y + secondBendHeight, z + dz, height, log, leaves);
        }

        return true;
    }

    @Override
    protected boolean canPlace(ChunkManager level, PRandom random, int x, int y, int z, int height) {
        for (int dy = 0; dy <= height + 1; dy++) {
            if (y + dy < 0 || y + dy >= 256) {
                return false;
            }

            int radius = dy == 0 ? 0 : 2;
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
    protected void placeLeaves(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if ((abs(dx) != 3 || abs(dz) != 3) && this.test(level.getBlockState(x + dx, y, z + dz, 0))) {
                    level.setBlockAt(x + dx, y, z + dz, 0, leaves);
                }
            }
        }

        y++;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if ((abs(dx) != 2 || abs(dz) != 2) && this.test(level.getBlockState(x + dx, y, z + dz, 0))) {
                    level.setBlockAt(x + dx, y, z + dz, 0, leaves);
                }
            }
        }
    }
}
