package org.cloudburstmc.server.level.light.oracle;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;

@UtilityClass
public class OracleSkyLightEngine {

    public static List<Vector3i> initialize(OracleLightAccess access, int chunkX, int chunkZ) {
        List<Vector3i> updates = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int light = 15;
                OracleLightAccess.Sample above = new OracleLightAccess.Sample(BlockStates.AIR, BlockStates.AIR, 15);

                for (int y = access.maxHeight() - 1; y >= access.minHeight(); y--) {
                    Vector3i position = Vector3i.from((chunkX << 4) + x, y, (chunkZ << 4) + z);
                    OracleLightAccess.Sample sample = access.read(position);

                    if (sample == null) {
                        break;
                    }

                    int opacity = sample.opacity();

                    if (OracleLightOcclusion.occludes(access.registry(), above, sample, Direction.DOWN)) {
                        light = 0;
                    } else {
                        light = Math.max(0, light - (light == 15 ? opacity : Math.max(1, opacity)));
                    }

                    above = sample;

                    if (sample.light() != light) {
                        access.write(position, light);
                    }

                    if (opacity < 15 && light < 15 || x == 0 || x == 15 || z == 0 || z == 15) {
                        updates.add(position);
                    }
                }
            }
        }

        return updates;
    }

    public static void update(OracleLightAccess access, Collection<Vector3i> changed) {
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
            OracleLightAccess.Sample sample = access.read(position);

            if (sample == null) {
                continue;
            }

            int opacity = sample.opacity();
            int light = 0;

            if (opacity < 15) {
                for (Direction direction : Direction.values()) {
                    Vector3i neighborPosition = direction.relative(position);
                    OracleLightAccess.Sample neighbor = access.read(neighborPosition);
                    int incoming = neighbor == null ? 0 : neighbor.light();

                    if (direction == Direction.UP && neighborPosition.getY() == access.maxHeight()) {
                        neighbor = new OracleLightAccess.Sample(BlockStates.AIR, BlockStates.AIR, 15);
                        incoming = 15;
                    }

                    int attenuation = direction == Direction.UP && incoming == 15 ? opacity : Math.max(1, opacity);

                    if (neighbor != null && incoming - attenuation > light
                            && !OracleLightOcclusion.occludes(access.registry(), neighbor, sample,
                            direction.getOpposite())) {
                        light = incoming - attenuation;
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
