package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
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
     * Writes the band's columns top-down with the channel's arrival rule. The
     * states above each cell are carried down the column instead of reread.
     *
     * @param band  the column band from {@code 0} to {@code bands - 1}
     * @param bands the number of column bands; must be positive
     */
    static void initializeBand(LightAccess access, int chunkX, int chunkZ, int band, int bands,
            LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int fromX = LightPositions.bandFromX(band, bands);
        int toX = LightPositions.bandToX(band, bands);
        LightStateCache states = access.states();
        for (int x = fromX; x < toX; x++) {
            for (int z = 0; z < 16; z++) {
                int light = 15;
                BlockState abovePrimary = BlockStates.AIR;
                BlockState aboveSecondary = BlockStates.AIR;
                for (int y = access.maxHeight() - 1; y >= access.minHeight(); y--) {
                    BlockState primary = access.primary(minX + x, y, minZ + z);
                    BlockState secondary = access.secondary(minX + x, y, minZ + z);
                    light = LightChannel.SKY.arrive(states, abovePrimary, aboveSecondary,
                            primary, secondary, Direction.DOWN, light);
                    if (access.light(minX + x, y, minZ + z) != light) {
                        access.setLight(minX + x, y, minZ + z, light);
                    }
                    abovePrimary = primary;
                    aboveSecondary = secondary;
                }
            }
        }
    }

    /**
     * Sources the lit cells of the band's columns that could raise a
     * neighbour. Runs once every column holds its top-down value. The border
     * sources run with the last band.
     *
     * @param band  the column band from {@code 0} to {@code bands - 1}
     * @param bands the number of column bands; must be positive
     */
    static void spreadBand(LightAccess access, int chunkX, int chunkZ, int band, int bands,
            LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int fromX = LightPositions.bandFromX(band, bands);
        int toX = LightPositions.bandToX(band, bands);
        for (int x = fromX; x < toX; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = access.minHeight(); y < access.maxHeight(); y++) {
                    int level = access.light(minX + x, y, minZ + z);
                    if (level > 0 && needsSpread(access, minX + x, y, minZ + z, level)) {
                        into.source(LightPositions.pack(minX + x, y, minZ + z));
                    }
                }
            }
        }
        if (band == bands - 1) {
            LightPositions.forNeighbourBorder(chunkX, chunkZ, access.minHeight(), access.maxHeight(),
                    (x, y, z, intoChunk) -> sourceIfLit(access, into, x, y, z, intoChunk));
        }
    }

    private static boolean needsSpread(LightAccess access, int x, int y, int z, int level) {
        int minHeight = access.minHeight();
        int maxHeight = access.maxHeight();
        for (int i = 0; i < LightChannel.DIRECTIONS.length; i++) {
            Direction direction = LightChannel.DIRECTIONS[i];
            if (direction == Direction.UP) {
                continue;
            }
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }
            // Arrivals never exceed the source level, so a neighbour already
            // at or above it cannot rise from here.
            if (access.light(nx, ny, nz) >= level) {
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
        // Arrivals never exceed the source level, so an inside neighbour
        // already at or above it cannot rise from here.
        if (access.light(toX, fromY, toZ) >= level) {
            return;
        }
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
