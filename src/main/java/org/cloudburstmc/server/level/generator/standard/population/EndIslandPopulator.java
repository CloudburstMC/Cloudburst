package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public class EndIslandPopulator extends ChancePopulator.Column {
    public static final Identifier ID = Identifier.fromString("cloudburst:end_island");

    @JsonProperty
    protected IntRange height;

    @JsonProperty
    protected BlockSelector block;

    @JsonProperty
    protected IntRange radius;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.height, "height must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
        Objects.requireNonNull(this.radius, "radius must be set!");
    }

    @Override
    protected void populate0(PRandom random, ChunkManager level, int blockX, int blockZ) {
        final int blockY = this.height.rand(random);
        final BlockState block = this.block.selectWeighted(random);
        double radius = this.radius.rand(random);

        for (int dy = 0; radius > 0.5d; dy--) {
            if (blockY + dy < 0) {
                return;
            }

            for (int f = floorI(-radius), c = ceilI(radius), dx = f; dx <= c; dx++) {
                for (int dz = f; dz <= c; dz++) {
                    if (dx * dx + dz * dz <= (radius + 1.0d) * (radius + 1.0d)) {
                        level.setBlockAt(blockX + dx, blockY + dy, blockZ + dz, 0, block);
                    }
                }
            }

            radius -= random.nextInt(2) + 0.5d;
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
