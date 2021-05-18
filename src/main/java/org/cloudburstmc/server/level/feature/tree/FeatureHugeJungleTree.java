package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static net.daporkchop.lib.common.math.PMath.floorI;

/**
 * Generates a huge jungle tree.
 *
 * @author DaPorkchop_
 */
public class FeatureHugeJungleTree extends FeatureHugeTree {
    public static final IntRange DEFAULT_HEIGHT = new IntRange(12, 31);

    public FeatureHugeJungleTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureHugeJungleTree(@NonNull IntRange height, BlockSelector log, BlockSelector leaves) {
        super(height, log, leaves);
    }

    @Override
    protected void placeLeaves(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        for (int dy = -2; dy <= 0; dy++) {
            this.placeCircularLeafLayer(level, x, y + height + dy, z, 3 - dy, leaves);
        }
    }

    @Override
    protected void placeTrunk(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        super.placeTrunk(level, random, x, y, z, height, log, leaves);

        //vines
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    this.placeVines(level, random, x + dx, y + dy, z + dz, Direction.NORTH);
                    this.placeVines(level, random, x + dx, y + dy, z + dz, Direction.SOUTH);
                    this.placeVines(level, random, x + dx, y + dy, z + dz, Direction.EAST);
                    this.placeVines(level, random, x + dx, y + dy, z + dz, Direction.WEST);
                }
            }
        }

        //branches
        for (int dy = height - 2 - random.nextInt(4), limit = height >> 1; dy > limit; dy -= 2 + random.nextInt(4)) {
            double dir = random.nextDouble() * Math.PI * 2.0d;
            double dirCos = cos(dir);
            double dirSin = sin(dir);

            int dx = 0;
            int dz = 0;

            for (int branchLength = 0; branchLength < 5; branchLength++) {
                dx = floorI(1.5d + dirCos * branchLength);
                dz = floorI(1.5d + dirSin * branchLength);
                int ddy = (branchLength >> 1) - 3;
                if (this.test(level.getBlockState(x + dx, y + dy + ddy, z + dz, 0))) {
                    level.setBlockState(x + dx, y + dy + ddy, z + dz, 0, log);
                }
            }

            for (int ddy = -(1 + random.nextInt(2)); ddy <= 0; ddy++) {
                this.placeCircularLeafLayer(level, x + dx, y + dy + ddy, z + dz, 1 - ddy, leaves);
            }
        }
    }

    protected void placeVines(ChunkManager level, PRandom random, int x, int y, int z, Direction face) {
        x -= face.getUnitVector().getX();
        z -= face.getUnitVector().getZ();
        if (random.nextInt(4) != 0 && this.test(level.getBlockState(x, y, z, 0))) {
            level.setBlockState(x, y, z, 0, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, face.getIndex()));
        }
    }
}
