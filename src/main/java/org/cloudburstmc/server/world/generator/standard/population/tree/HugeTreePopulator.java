package org.cloudburstmc.server.world.generator.standard.population.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.world.ChunkManager;
import org.cloudburstmc.server.world.chunk.IChunk;
import org.cloudburstmc.server.world.feature.WorldFeature;
import org.cloudburstmc.server.world.feature.tree.GenerationTreeSpecies;
import org.cloudburstmc.server.world.generator.standard.StandardGenerator;
import org.cloudburstmc.server.world.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.world.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.Math.min;
import static net.daporkchop.lib.random.impl.FastPRandom.mix32;
import static net.daporkchop.lib.random.impl.FastPRandom.mix64;

/**
 * A populator that places simple trees, with a similar shape to vanilla oak/birch trees.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class HugeTreePopulator extends AbstractTreePopulator {
    public static final Identifier ID = Identifier.fromString("cloudburst:huge_tree");

    @JsonProperty
    protected BlockSelector below;

    protected WorldFeature[] types;

    @JsonProperty
    protected boolean grid;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.types, "type must be set!");
        Objects.requireNonNull(this.below, "below must be set!");
    }

    @Override
    public void populate(PRandom random, ChunkManager level, int blockX, int blockZ) {
        if (this.grid) {
            if ((mix32(mix64(this.seed() + (blockX >> 2)) + (blockZ >> 2)) & 0xF) != (((blockX & 3) << 2) | (blockZ & 3))) {
                //bitwise magic!
                //splits the world into 4x4 tiles with a single tree in each one
                return;
            }

            final BlockFilter replace = this.replace;
            final BlockFilter on = this.on;

            final int max = min(this.height.max - 1, 254);
            final int min = this.height.min;

            IChunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
            BlockState lastId = chunk.getBlock(blockX & 0xF, max + 1, blockZ & 0xF, 0);
            for (int y = max; y >= min; y--) {
                BlockState id = chunk.getBlock(blockX & 0xF, y, blockZ & 0xF, 0);

                if (replace.test(lastId) && on.test(id)) {
                    this.placeTree(random, level, blockX, y, blockZ);
                }

                lastId = id;
            }
        } else {
            super.populate(random, level, blockX, blockZ);
        }
    }

    @Override
    protected void placeTree(PRandom random, ChunkManager level, int x, int y, int z) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockState test = level.getBlockAt(x + dx, y, z + dz, 0);
                if (!this.on.test(test) && (!this.replace.test(test) || !this.on.test(level.getBlockAt(x + dx, y - 1, z + dz, 0)))) {
                    return;
                }
            }
        }

        if (this.types[random.nextInt(this.types.length)].place(level, random, x, y + 1, z)) {
            BlockState below = this.below.selectWeighted(random);
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    level.setBlockAt(x + dx, y, z + dz, 0, below);
                    level.setBlockAt(x + dx, y - 1, z + dz, 0, below);
                }
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @JsonSetter("type")
    private void setType(@NonNull ConfigTree type) {
        this.types = new WorldFeature[]{type.build()};
    }

    @JsonSetter("types")
    private void setTypes(@NonNull ConfigTree[] types) {
        Preconditions.checkArgument(types.length > 0, "types may not be empty!");
        this.types = Arrays.stream(types)
                .map(ConfigTree::build)
                .toArray(WorldFeature[]::new);
    }

    @JsonDeserialize
    private static final class ConfigTree {
        private WorldFeature feature;

        @JsonCreator
        public ConfigTree(String species) {
            this.feature = Preconditions.checkNotNull(GenerationTreeSpecies.valueOf(species.toUpperCase()).getHugeGenerator(), "%s does not support huge trees!", species);
        }

        public WorldFeature build() {
            return this.feature;
        }
    }
}
