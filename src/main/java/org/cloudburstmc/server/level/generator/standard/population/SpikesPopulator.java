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

import static java.lang.Math.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * Places large spikes in the world.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class SpikesPopulator extends ChancePopulator.Column {
    public static final Identifier ID = Identifier.fromString("cloudburst:spikes");

    @JsonProperty
    protected BlockFilter on;

    @JsonProperty
    protected BlockFilter replace = BlockFilter.AIR;

    @JsonProperty
    protected BlockSelector block;

    @JsonProperty
    protected IntRange height;

    @JsonProperty
    protected IntRange tallHeight;

    @JsonProperty
    protected double tallChance = 0.1d;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.on, "on must be set!");
        Objects.requireNonNull(this.replace, "replace must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
        Objects.requireNonNull(this.height, "height must be set!");
        this.tallHeight = this.tallHeight == null ? this.height : this.tallHeight;
    }

    @Override
    protected void populate0(PRandom random, ChunkManager level, int x, int z) {
        int y = level.getChunk(x >> 4, z >> 4).getHighestBlock(x & 0xF, z & 0xF);
        if (y < 0 || !this.on.test(level.getBlockAt(x, y, z, 0))) {
            return;
        }

        final BlockFilter replace = this.replace;
        final BlockState block = this.block.selectWeighted(random);

        int height = this.height.rand(random);
        final int sink = (height >> 2) + random.nextInt(2);
        if (random.nextDouble() < this.tallChance) {
            y += this.tallHeight.rand(random);
        }

        for (int dy = 0; dy < height; dy++) {
            double rf = (1.0d - dy / (double) height) * sink;
            int radius = ceilI(rf);
            rf *= rf;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double fx = abs(dx) - 0.25d;
                    double fz = abs(dz) - 0.25d;
                    if (((dx == 0 && dz == 0) || fx * fx + fz * fz < rf)
                        && ((abs(dx) != radius && abs(dz) != radius) || random.nextInt(4) == 0)) {
                        if (y + dy < 255 && replace.test(level.getBlockAt(x + dx, y + dy, z + dz, 0))) {
                            level.setBlockAt(x + dx, y + dy, z + dz, 0, block);
                        }
                        if (dy != 0 && radius > 1 && y - dy < 255 && replace.test(level.getBlockAt(x + dx, y - dy, z + dz, 0))) {
                            level.setBlockAt(x + dx, y - dy, z + dz, 0, block);
                        }
                    }
                }
            }
        }

        for (; y >= 0; y--) {
            BlockState test = level.getBlockAt(x, y, z, 0);
            if (test != block && !replace.test(test)) {
                return;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx == 0 || dz == 0 || random.nextBoolean()) && replace.test(level.getBlockAt(x + dx, y, z + dz, 0))) {
                        level.setBlockAt(x + dx, y, z + dz, 0, block);
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
