package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;

/**
 * Places large spikes in the world.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class BlobPopulator extends ChancePopulator.Column {
    public static final Identifier ID = Identifier.fromString("cloudburst:blob");

    @JsonProperty
    protected BlockFilter on;

    @JsonProperty
    protected BlockFilter replace = BlockFilter.AIR;

    @JsonProperty
    protected BlockSelector block;

    @JsonProperty
    protected IntRange radius;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.on, "on must be set!");
        Objects.requireNonNull(this.replace, "replace must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
        Objects.requireNonNull(this.radius, "radius must be set!");
    }

    @Override
    protected void populate0(PRandom random, ChunkManager level, int blockX, int blockZ) {
        int y = level.getChunk(blockX >> 4, blockZ >> 4).getHighestBlock(blockX & 0xF, blockZ & 0xF);
        if (y < 0 || !this.on.test(level.getBlockAt(blockX, y, blockZ, 0))) {
            return;
        }

        final BlockFilter replace = this.replace;
        final BlockState block = this.block.selectWeighted(random);
        final int min = this.radius.min;

        for (int i = 0; i < 3; i++) {
            int vx = this.radius.rand(random);
            int vy = this.radius.rand(random);
            int vz = this.radius.rand(random);
            double g = (vx + vy + vz) * 0.333333333333d + 0.5d;
            g *= g;

            for (int dx = -vx; dx <= vx; dx++) {
                for (int dy = -vy; dy <= vy; dy++) {
                    if (y + dy < 0 || y + dy >= 256) {
                        continue;
                    }
                    for (int dz = -vz; dz <= vz; dz++) {
                        if (dx * dx + dy * dy + dz * dz <= g && replace.test(level.getBlockAt(blockX + dx, y + dy, blockZ + dz, 0))) {
                            level.setBlockAt(blockX + dx, y + dy, blockZ + dz, 0, block);
                        }
                    }
                }
            }

            blockX += random.nextInt(-(min + 1), min + 2);
            y -= min > 0 ? random.nextInt(min) : 0;
            blockZ += random.nextInt(-(min + 1), min + 2);
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
