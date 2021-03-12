package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.math.Direction;

/**
 * Generates normal trees, but with vines on the sides.
 *
 * @author DaPorkchop_
 */
public class FeatureJungleTree extends FeatureNormalTree {
    public FeatureJungleTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureJungleTree(@NonNull IntRange height, @NonNull BlockSelector wood, @NonNull BlockSelector leaves) {
        super(height, wood, leaves);
    }

    @Override
    protected void placeTrunk(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        super.placeTrunk(level, random, x, y, z, height, log, leaves);

        for (int dy = 0; dy < height; dy++) {
            this.placeVines(level, random, x, y + dy, z, Direction.NORTH);
            this.placeVines(level, random, x, y + dy, z, Direction.SOUTH);
            this.placeVines(level, random, x, y + dy, z, Direction.EAST);
            this.placeVines(level, random, x, y + dy, z, Direction.WEST);
        }
    }

    protected void placeVines(ChunkManager level, PRandom random, int x, int y, int z, Direction face) {
        x -= face.getUnitVector().getX();
        z -= face.getUnitVector().getZ();
        if (random.nextInt(4) != 0 && this.test(level.getBlockState(x, y, z, 0))) {
            level.setBlockAt(x, y, z, 0, BlockStates.VINE.withTrait(BlockTraits.FACING_DIRECTION, face));
        }
    }
}
