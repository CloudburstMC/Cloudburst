package org.cloudburstmc.server.level.generator.standard.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EndSpikeLayout {

    private static final int SPIKE_COUNT = 10;
    private static final int DISTANCE = 42;

    private EndSpikeLayout() {
    }

    public static List<Spike> create(long levelSeed) {
        long layoutSeed = new Random(levelSeed).nextLong() & 0xffffL;
        List<Integer> sizes = new ArrayList<>(SPIKE_COUNT);

        for (int size = 0; size < SPIKE_COUNT; size++) {
            sizes.add(size);
        }

        Collections.shuffle(sizes, new Random(layoutSeed));
        List<Spike> spikes = new ArrayList<>(SPIKE_COUNT);

        for (int index = 0; index < SPIKE_COUNT; index++) {
            double angle = 2 * (-Math.PI + Math.PI / SPIKE_COUNT * index);
            int size = sizes.get(index);

            spikes.add(new Spike(
                    (int) Math.floor(DISTANCE * Math.cos(angle)),
                    (int) Math.floor(DISTANCE * Math.sin(angle)),
                    2 + size / 3,
                    76 + size * 3,
                    size == 1 || size == 2)
            );
        }

        return List.copyOf(spikes);
    }

    public record Spike(int x, int z, int radius, int height, boolean guarded) {

        public boolean isInChunk(int chunkX, int chunkZ) {
            return (this.x >> 4) == chunkX && (this.z >> 4) == chunkZ;
        }
    }
}
