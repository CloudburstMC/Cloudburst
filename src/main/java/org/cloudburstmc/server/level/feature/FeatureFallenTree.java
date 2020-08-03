package org.cloudburstmc.server.level.feature;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author DaPorkchop_
 */
public class FeatureFallenTree extends ReplacingWorldFeature {
    protected final IntRange size;
    protected final Identifier logId;
    protected final int logType;
    protected final double vineChance;

    public FeatureFallenTree(@NonNull IntRange size, @NonNull Identifier logId, int logType, double vineChance) {
        this.size = size;
        this.logId = logId;
        this.logType = logType;
        this.vineChance = vineChance;
    }

    @Override
    public boolean place(ChunkManager level, PRandom random, int x, int y, int z) {
        if (y <= 0 || y >= 255) {
            return false;
        }

        final int size = this.size.rand(random);
        final Direction direction = Direction.Plane.HORIZONTAL.random(random);
        for (int i = 0; i < size; i++) {
            if (!this.test(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + direction.getXOffset() * i, y, z + direction.getZOffset() * i, 0)))
                    || this.testOrLiquid(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + direction.getXOffset() * i, y - 1, z + direction.getZOffset() * i, 0)))) {
                return false;
            }
        }

//        level.setBlockAt(x, y, z, 0, this.logId, this.logType);

//        int metaDirection = direction.getAxis() == Direction.Axis.X ? BlockBehaviorLog.EAST_WEST : BlockBehaviorLog.NORTH_SOUTH;
//        int log = BlockRegistry.get().getRuntimeId(this.logId, this.logType | metaDirection);
//        for (int i = random.nextInt(2) + 2; i < size; i++) {
//            org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt) * i, y + 1, z + direction.getZOffset() * i, 0)))) {
//                level.setBlockAt(x + direction.getXOffset() * i, y + 1, z + direction.getZOffset() * i, 0, random.nextBoolean() ? BlockTypes.BROWN_MUSHROOM : BlockTypes.RED_MUSHROOM, 0);
//            }
//
//            this.replaceGrassWithDirt(level, x + direction.getXOffset() * i, y - 1, z + direction.getZOffset() * i);
//        }
//
//        if (this.vineChance > 0.0d) {
//            if (random.nextDouble() < this.vineChance && this.test(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x - 1, y, z, 0)))) {
//                level.setBlockAt(x - 1, y, z, 0, BlockTypes.VINE, BlockBehaviorVine.EAST);
//            }
//            if (random.nextDouble() < this.vineChance && this.test(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + 1, y, z, 0)))) {
//                level.setBlockAt(x + 1, y, z, 0, BlockTypes.VINE, BlockBehaviorVine.WEST);
//            }
//            if (random.nextDouble() < this.vineChance && this.test(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x, y, z - 1, 0)))) {
//                level.setBlockAt(x, y, z - 1, 0, BlockTypes.VINE, BlockBehaviorVine.SOUTH);
//            }
//            if (random.nextDouble() < this.vineChance && this.test(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x, y, z + 1, 0)))) {
//                level.setBlockAt(x, y, z + 1, 0, BlockTypes.VINE, BlockBehaviorVine.NORTH);
//            }
//        }

        return true;
    }
}
