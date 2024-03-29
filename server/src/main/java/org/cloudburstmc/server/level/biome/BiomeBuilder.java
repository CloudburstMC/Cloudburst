package org.cloudburstmc.server.level.biome;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BiomeBuilder {
    public static BiomeBuilder builder() {
        return new BiomeBuilder();
    }

    public static BiomeBuilder from(@NonNull CloudBiome biome) {
        return builder();
    }

    protected Identifier id;
    protected Set<Identifier> tags = new HashSet<>();
    protected float temperature = 0.5f;
    protected float downfall = 0.5f;

    public BiomeBuilder setId(Identifier id) {
        this.id = id;
        return this;
    }

    public BiomeBuilder setTags(Collection<Identifier> tags) {
        this.tags = new HashSet<>(tags);
        return this;
    }

    public BiomeBuilder setTemperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public BiomeBuilder setDownfall(float downfall) {
        this.downfall = downfall;
        return this;
    }

    public CloudBiome build() {
        return new CloudBiome(this.id, this.tags, this.temperature, this.downfall);
    }
}