package org.cloudburstmc.server.level.generator.standard.population.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.feature.tree.GenerationTreeSpecies;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * A populator that places simple trees, with a similar shape to vanilla oak/birch trees.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class TreePopulator extends AbstractTreePopulator {
    public static final Identifier ID = Identifier.parse("cloudburst:tree");

    protected WorldFeature[] types;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.types, "type must be set!");
    }

    @Override
    protected void placeTree(RandomGenerator random, ChunkManager level, int x, int y, int z) {
        this.types[random.nextInt(this.types.length)].place(level, random, x, y + 1, z);
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
        private final GenerationTreeSpecies species;
        private IntRange height;
        private boolean fallen;

        @JsonCreator
        public ConfigTree(String species) {
            this.species = GenerationTreeSpecies.valueOf(species.toUpperCase());
        }

        @JsonCreator
        public ConfigTree(
                @JsonProperty(value = "species", required = true) @NonNull String species,
                @JsonProperty(value = "height") IntRange height,
                @JsonProperty(value = "fallen") boolean fallen) {
            this(species);

            this.height = height;
            this.fallen = fallen;
        }

        public WorldFeature build() {
            if (this.fallen) {
                return Preconditions.checkNotNull(this.species.getFallenGenerator(), "%s does not support fallen trees!", this.species.name());
            } else if (this.height != null) {
                return Preconditions.checkNotNull(this.species.getDefaultGenerator(this.height), "%s does not support huge trees!", this.species.name());
            } else {
                return Preconditions.checkNotNull(this.species.getDefaultGenerator(), "%s does not support normal trees!", this.species.name());
            }
        }
    }
}
