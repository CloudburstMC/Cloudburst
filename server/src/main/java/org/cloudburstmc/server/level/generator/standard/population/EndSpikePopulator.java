package org.cloudburstmc.server.level.generator.standard.population;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.BlockStateRegion;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.random.RandomGenerator;

@JsonDeserialize
public class EndSpikePopulator extends AbstractGenerationPass implements Populator {

    public static final Identifier ID = Identifier.parse("cloudburst:end_spikes");

    private List<EndSpikeLayout.Spike> spikes;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        this.spikes = EndSpikeLayout.create(levelSeed);
    }

    @Override
    public void populate(RandomGenerator random, GenerationRegion level, int blockX, int blockZ) {
        for (EndSpikeLayout.Spike spike : this.spikes) {
            if (spike.x() == blockX && spike.z() == blockZ) {
                placeSpike(level, spike);
                return;
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    public static void placeSpike(BlockStateRegion level, EndSpikeLayout.Spike spike) {
        int radiusSquared = spike.radius() * spike.radius() + 1;
        for (int x = spike.x() - spike.radius(); x <= spike.x() + spike.radius(); x++) {
            for (int z = spike.z() - spike.radius(); z <= spike.z() + spike.radius(); z++) {
                int dx = x - spike.x();
                int dz = z - spike.z();
                boolean insidePillar = dx * dx + dz * dz <= radiusSquared;
                for (int y = 0; y <= spike.height() + 10; y++) {
                    if (insidePillar && y < spike.height()) {
                        level.setBlockState(x, y, z, BlockStates.OBSIDIAN);
                    } else if (y > 65) {
                        level.setBlockState(x, y, z, BlockStates.AIR);
                    }
                }
            }
        }

        level.setBlockState(spike.x(), spike.height(), spike.z(), BlockStates.BEDROCK);
        level.setBlockState(spike.x(), spike.height() + 1, spike.z(), BlockStates.FIRE);
        if (spike.guarded()) {
            placeGuard(level, spike);
        }
    }

    private static void placeGuard(BlockStateRegion level, EndSpikeLayout.Spike spike) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2 || y == 3) {
                        level.setBlockState(spike.x() + x, spike.height() + y, spike.z() + z, BlockStates.IRON_BARS);
                    }
                }
            }
        }
    }
}
