package org.cloudburstmc.server.level.feature;

import lombok.NonNull;
import net.daporkchop.lib.common.util.PValidation;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;

import java.util.Random;

import static java.lang.Math.abs;

/**
 * Generates a fully grown chorus tree.
 *
 * @author DaPorkchop_
 */
public class FeatureChorusTree extends ReplacingWorldFeature {
    public static final IntRange DEFAULT_BRANCH_HEIGHT = new IntRange(1, 4);
    public static final int DEFAULT_MAX_RECURSION = 4;
    public static final int DEFAULT_MAX_OVERHANG = 8;

    protected final IntRange branchHeight;
    protected final int maxRecursion;
    protected final int maxOverhang;

    public FeatureChorusTree(@NonNull IntRange branchHeight, int maxRecursion, int maxOverhang) {
        this.branchHeight = branchHeight;
        this.maxRecursion = PValidation.positive(maxRecursion);
        this.maxOverhang = PValidation.positive(maxOverhang);
    }

    @Override
    public boolean place(ChunkManager level, PRandom random, int x, int y, int z) {
        if (this.test(level.getBlockState(x, y, z, 0)) && this.place0(level, random, x, y, z, 0, 0, 0)) {
            level.setBlockState(x, y, z, 0, BlockStates.CHORUS_PLANT);
            return true;
        } else {
            return false;
        }
    }

    private boolean place0(ChunkManager level, PRandom random, int x, int y, int z, int depth, int deltaX, int deltaZ) {
        final int branchHeight = this.branchHeight.rand(random) + (depth == 0 ? 1 : 0);

        for (int dy = 1; dy <= branchHeight; dy++) {
            if (!this.allNeighborsMatch(level, x, y + dy, z, BlockFilter.AIR)) {
                return false;
            }
        }

        for (int dy = 1; dy <= branchHeight; dy++) {
            level.setBlockState(x, y + dy, z, 0, BlockStates.CHORUS_PLANT);
        }

        boolean generatedBranch = false;
        y += branchHeight;
        if (depth < this.maxRecursion) {
            for (int i = random.nextInt(4) - (depth == 0 ? 0 : 1); i >= 0; i--) {
                final Direction face = Direction.Plane.HORIZONTAL.random((Random) random);
                final int dx = face.getUnitVector().getX();
                final int dz = face.getUnitVector().getZ();

                if (abs(deltaX + dx) < this.maxOverhang && abs(deltaZ + dz) < this.maxOverhang
                        && level.getBlockState(x + dx, y, z + dz, 0) == BlockStates.AIR
                        && level.getBlockState(x + dx, y - 1, z + dz, 0) == BlockStates.AIR
                        && this.allNeighborsMatch(level, x + dx, y, z + dz, BlockFilter.AIR, face.getOpposite())
                        && this.place0(level, random, x + dx, y, z + dz, depth + 1, deltaX + dx, deltaZ + dz)) {
                    level.setBlockState(x + dx, y, z + dz, 0, BlockStates.CHORUS_PLANT);
                    generatedBranch = true;
                }
            }
        }

        if (!generatedBranch) {
            level.setBlockState(x, y, z, 0, BlockStates.CHORUS_FLOWER.withTrait(BlockTraits.CHORUS_AGE, 5));
        }

        return true;
    }
}
