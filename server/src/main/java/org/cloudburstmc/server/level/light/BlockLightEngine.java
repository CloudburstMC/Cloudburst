package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class BlockLightEngine {

    public static void update(Level level, Collection<Vector3i> changed) {
        update(new ChunkLightAccess(level, LightAccess.Type.BLOCK), changed);
    }

    public static List<Vector3i> initialize(LightAccess access, int chunkX, int chunkZ) {
        List<Vector3i> updates = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = access.minHeight(); y < access.maxHeight(); y++) {
                    Vector3i position = Vector3i.from((chunkX << 4) + x, y, (chunkZ << 4) + z);
                    LightAccess.Sample sample = access.read(position);
                    if (sample == null) {
                        continue;
                    }

                    int emission = sample.emission();
                    if (sample.light() != emission) {
                        access.write(position, emission);
                        updates.add(position);
                    } else if (emission > 0 || x == 0 || x == 15 || z == 0 || z == 15) {
                        updates.add(position);
                    }
                }
            }
        }

        return updates;
    }

    public static void update(LightAccess access, Collection<Vector3i> changed) {
        ArrayDeque<Vector3i> queue = new ArrayDeque<>();
        Set<Vector3i> pending = new HashSet<>();
        for (Vector3i position : changed) {
            enqueue(queue, pending, position);
            for (Direction direction : Direction.values()) {
                enqueue(queue, pending, direction.relative(position));
            }
        }

        while (!queue.isEmpty()) {
            Vector3i position = queue.removeFirst();
            pending.remove(position);
            LightAccess.Sample sample = access.read(position);
            if (sample == null) {
                continue;
            }

            int light = sample.emission();
            int attenuation = Math.max(1, sample.opacity());
            if (light < 15 && attenuation < 15) {
                for (Direction direction : Direction.values()) {
                    LightAccess.Sample neighbor = access.read(direction.relative(position));
                    if (neighbor != null && neighbor.light() - attenuation > light
                            && !LightOcclusion.occludes(access.registry(), neighbor, sample, direction.getOpposite())) {
                        light = Math.max(light, neighbor.light() - attenuation);
                    }
                }
            }

            light = Math.clamp(light, 0, 15);
            if (sample.light() == light) {
                continue;
            }

            access.write(position, light);
            for (Direction direction : Direction.values()) {
                enqueue(queue, pending, direction.relative(position));
            }
        }
    }

    private static void enqueue(ArrayDeque<Vector3i> queue, Set<Vector3i> pending, Vector3i position) {
        if (pending.add(position)) {
            queue.addLast(position);
        }
    }
}
