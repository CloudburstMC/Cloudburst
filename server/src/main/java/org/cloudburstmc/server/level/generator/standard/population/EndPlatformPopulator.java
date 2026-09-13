package org.cloudburstmc.server.level.generator.standard.population;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.feature.EndPlatformFeature;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.random.RandomGenerator;

@JsonDeserialize
public final class EndPlatformPopulator extends AbstractGenerationPass implements Populator {

    public static final Identifier ID = Identifier.parse("cloudburst:end_platform");

    @Override
    public void populate(RandomGenerator random, GenerationRegion region, int blockX, int blockZ) {
        if (blockX == EndPlatformFeature.SPAWN.getX() && blockZ == EndPlatformFeature.SPAWN.getZ()) {
            EndPlatformFeature.generate(region);
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
