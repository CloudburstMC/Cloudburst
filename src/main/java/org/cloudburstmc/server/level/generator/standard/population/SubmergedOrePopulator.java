package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;

/**
 * Generates underwater ore veins.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class SubmergedOrePopulator extends ChancePopulator.Column {
    public static final Identifier ID = Identifier.fromString("cloudburst:ore_submerged");

    @JsonProperty
    protected BlockFilter replace;

    @JsonProperty
    protected BlockFilter start;

    @JsonProperty
    protected IntRange radius;

    @JsonProperty
    protected BlockSelector block;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.replace, "replace must be set!");
        Objects.requireNonNull(this.start, "start must be set!");
        Objects.requireNonNull(this.radius, "radius must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
    }

    @Override
    protected void populate0(PRandom random, ChunkManager level, int blockX, int blockZ) {
        IChunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
        int y = chunk.getHighestBlock(blockX & 0xF, blockZ & 0xF);
        for (; y > 0; y--) {
            if (this.replace.test(chunk.getBlock(blockX & 0xF, y, blockZ & 0xF, 0))) {
                break;
            }
        }
        if (y <= 0 || y >= 255 || !this.start.test(level.getBlockAt(blockX, y + 1, blockZ, 0))) {
            return;
        }

        final int radius = this.radius.rand(random);
        final int radiusSq = radius * radius;
        final BlockState block = this.block.selectWeighted(random);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radiusSq) {
                    for (int dy = -1; dy < 1; dy++) {
                        if (this.replace.test(level.getBlockAt(blockX + dx, y + dy, blockZ + dz, 0))) {
                            level.setBlockAt(blockX + dx, y + dy, blockZ + dz, 0, block);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
