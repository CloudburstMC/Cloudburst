package org.cloudburstmc.server.world.generator.standard.population.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudburstmc.server.world.generator.standard.StandardGenerator;
import org.cloudburstmc.server.world.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.world.generator.standard.population.ChancePopulator;

import java.util.Objects;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public abstract class AbstractReplacingPopulator extends ChancePopulator.Column {
    @JsonProperty
    protected BlockFilter replace;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.replace, "replace must be set!");
    }
}
