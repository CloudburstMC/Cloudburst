package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;

@UtilityClass
public class SkyLightEngine {

    private static final List<SkyLightKeyframe> SKY_LIGHT_CYCLE = List.of(
            new SkyLightKeyframe(133, 15),
            new SkyLightKeyframe(11867, 15),
            new SkyLightKeyframe(13670, 4),
            new SkyLightKeyframe(22330, 4)
    );

    public static int skyDarkness(long time, float rainLevel, float thunderLevel) {
        float light = skyLightLevel(time);
        float rain = Math.clamp(rainLevel, 0, 1);
        float thunder = Math.clamp(thunderLevel, 0, 1) * rain;
        light += (4 - light) * (rain - thunder) * 0.3125f;
        light += (4 - light) * thunder * 0.52734375f;
        return (int) (15 - light);
    }

    public static void update(Level level, Collection<Vector3i> changed) {
        update(new ChunkLightAccess(level, LightAccess.Type.SKY), changed);
    }

    public static List<Vector3i> initialize(LightAccess access, int chunkX, int chunkZ) {
        List<Vector3i> updates = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int light = 15;
                LightAccess.Sample above = new LightAccess.Sample(BlockStates.AIR, BlockStates.AIR, 15);
                for (int y = access.maxHeight() - 1; y >= access.minHeight(); y--) {
                    Vector3i position = Vector3i.from((chunkX << 4) + x, y, (chunkZ << 4) + z);
                    LightAccess.Sample sample = access.read(position);
                    if (sample == null) {
                        break;
                    }

                    int opacity = sample.opacity();
                    if (LightOcclusion.occludes(access.registry(), above, sample, Direction.DOWN)) {
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

            int opacity = sample.opacity();
            int light = 0;
            if (opacity < 15) {
                for (Direction direction : Direction.values()) {
                    Vector3i neighborPosition = direction.relative(position);
                    LightAccess.Sample neighbor = access.read(neighborPosition);
                    int incoming = neighbor == null ? 0 : neighbor.light();
                    if (direction == Direction.UP && neighborPosition.getY() == access.maxHeight()) {
                        neighbor = new LightAccess.Sample(BlockStates.AIR, BlockStates.AIR, 15);
                        incoming = 15;
                    }

                    int attenuation = direction == Direction.UP && incoming == 15 ? opacity : Math.max(1, opacity);
                    if (neighbor != null && incoming - attenuation > light && !LightOcclusion.occludes(access.registry(), neighbor, sample, direction.getOpposite())) {
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

    private static float skyLightLevel(long time) {
        int tick = DayCycle.timeOfDay(time);
        if (tick < SKY_LIGHT_CYCLE.getFirst().tick()) {
            tick += DayCycle.TICKS_PER_DAY;
        }

        for (int i = 0; i < SKY_LIGHT_CYCLE.size(); i++) {
            SkyLightKeyframe start = SKY_LIGHT_CYCLE.get(i);
            SkyLightKeyframe end = SKY_LIGHT_CYCLE.get((i + 1) % SKY_LIGHT_CYCLE.size());
            int endTick = end.tick() + (i == SKY_LIGHT_CYCLE.size() - 1 ? DayCycle.TICKS_PER_DAY : 0);
            if (tick < endTick) {
                float progress = (float) (tick - start.tick()) / (endTick - start.tick());
                return start.light() + (end.light() - start.light()) * progress;
            }
        }

        throw new IllegalStateException("Sky light cycle does not cover tick " + tick);
    }

    private static void enqueue(ArrayDeque<Vector3i> queue, Set<Vector3i> pending, Vector3i position) {
        if (pending.add(position)) {
            queue.addLast(position);
        }
    }

    private record SkyLightKeyframe(int tick, float light) {
    }
}
