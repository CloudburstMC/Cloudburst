package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.util.Direction;

import java.util.List;

/**
 * Writes a chunk's sky light from scratch and seeds propagation.
 */
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

    /**
     * Writes each column top-down with the channel's arrival rule, sources
     * lit cells that could raise a neighbour, and sources lit neighbour cells
     * touching the chunk border.
     */
    static void initialize(LightAccess access, int chunkX, int chunkZ, LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int light = 15;
                for (int y = access.maxHeight() - 1; y >= access.minHeight(); y--) {
                    light = LightChannel.SKY.arrive(access, minX + x, y + 1, minZ + z, Direction.DOWN, light);
                    if (access.light(minX + x, y, minZ + z) != light) {
                        access.setLight(minX + x, y, minZ + z, light);
                    }
                }
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = access.minHeight(); y < access.maxHeight(); y++) {
                    int level = access.light(minX + x, y, minZ + z);
                    if (level > 0 && needsSpread(access, minX + x, y, minZ + z, level)) {
                        into.source(LightPositions.pack(minX + x, y, minZ + z));
                    }
                }
            }
        }
        LightRegion.forNeighbourBorder(chunkX, chunkZ, access.minHeight(), access.maxHeight(),
                (x, y, z, intoChunk) -> sourceIfLit(access, into, x, y, z, intoChunk));
    }

    private static boolean needsSpread(LightAccess access, int x, int y, int z, int level) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                continue;
            }
            if (access.light(nx, ny, nz) < LightChannel.SKY.arrive(access, x, y, z, direction, level)) {
                return true;
            }
        }
        return false;
    }

    private static void sourceIfLit(LightAccess access, LightPropagation into,
            int fromX, int fromY, int fromZ, Direction intoChunk) {
        if (!access.isLoaded(fromX, fromZ)) {
            return;
        }
        int level = access.light(fromX, fromY, fromZ);
        if (level <= 0) {
            return;
        }
        int toX = fromX + intoChunk.getStepX();
        int toZ = fromZ + intoChunk.getStepZ();
        if (LightChannel.SKY.arrive(access, fromX, fromY, fromZ, intoChunk, level) > access.light(toX, fromY, toZ)) {
            into.source(LightPositions.pack(fromX, fromY, fromZ));
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

    private record SkyLightKeyframe(int tick, float light) {
    }
}
