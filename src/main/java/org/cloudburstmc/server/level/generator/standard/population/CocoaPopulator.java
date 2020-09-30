package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public class CocoaPopulator extends ChancePopulator {
    public static final Identifier ID = Identifier.fromString("cloudburst:cocoa");

    @JsonProperty
    protected IntRange height = IntRange.WHOLE_WORLD;

    @JsonProperty
    protected BlockFilter on;

    @JsonProperty
    protected BlockFilter replace = BlockFilter.AIR;

    public boolean avoidDouble = false;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.height, "height must be set!");
        Objects.requireNonNull(this.on, "on must be set!");
        Objects.requireNonNull(this.replace, "replace must be set!");
    }

    @Override
    public void populate(PRandom random, ChunkManager level, int blockX, int blockZ) {
        final double chance = this.chance;
        final BlockFilter replace = this.replace;
        final BlockFilter on = this.on;
        final boolean avoidDouble = this.avoidDouble;

        final IChunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
        for (int y = this.height.min, max = this.height.max; y < max; y++) {
            if (random.nextDouble() >= chance || !replace.test(chunk.getBlock(blockX & 0xF, y, blockZ & 0xF, 0))) {
                continue;
            }

            if (on.test(level.getBlockAt(blockX - 1, y, blockZ, 0))) {
                if (!avoidDouble || !on.test(level.getBlockAt(blockX - 2, y, blockZ, 0))) {
                    level.setBlockAt(blockX, y, blockZ, 0, BlockStates.COCOA.withTrait(BlockTraits.DIRECTION, Direction.EAST));
                }
            } else if (on.test(level.getBlockAt(blockX + 1, y, blockZ, 0))) {
                if (!avoidDouble || !on.test(level.getBlockAt(blockX + 2, y, blockZ, 0))) {
                    level.setBlockAt(blockX, y, blockZ, 0, BlockStates.COCOA.withTrait(BlockTraits.DIRECTION, Direction.WEST));
                }
            } else if (on.test(level.getBlockAt(blockX, y, blockZ - 1, 0))) {
                if (!avoidDouble || !on.test(level.getBlockAt(blockX, y, blockZ - 2, 0))) {
                    level.setBlockAt(blockX, y, blockZ, 0, BlockStates.COCOA.withTrait(BlockTraits.DIRECTION, Direction.SOUTH));
                }
            } else if (on.test(level.getBlockAt(blockX, y, blockZ + 1, 0))) {
                if (!avoidDouble || !on.test(level.getBlockAt(blockX, y, blockZ + 2, 0))) {
                    level.setBlockAt(blockX, y, blockZ, 0, BlockStates.COCOA.withTrait(BlockTraits.DIRECTION, Direction.NORTH));
                }
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
