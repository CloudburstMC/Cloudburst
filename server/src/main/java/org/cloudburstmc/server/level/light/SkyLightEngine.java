package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.util.Direction;

import java.util.List;

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

    public static void initialize(LightAccess access, int chunkX, int chunkZ, LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        LightStateCache states = access.states();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int light = 15;
                BlockState abovePrimary = BlockStates.AIR;
                BlockState aboveSecondary = BlockStates.AIR;
                for (int y = access.maxHeight() - 1; y >= access.minHeight(); y--) {
                    BlockState primary = access.primary(minX + x, y, minZ + z);
                    BlockState secondary = access.secondary(minX + x, y, minZ + z);
                    int opacity = states.opacity(primary, secondary);
                    if (states.occludes(abovePrimary, aboveSecondary, primary, secondary, Direction.DOWN)) {
                        light = 0;
                    } else {
                        light = Math.max(0, light - (light == 15 ? opacity : Math.max(1, opacity)));
                    }
                    abovePrimary = primary;
                    aboveSecondary = secondary;
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
                    if (level > 0 && needsSpread(access, into, minX + x, y, minZ + z, level)) {
                        into.source(LightPositions.pack(minX + x, y, minZ + z));
                    }
                }
            }
        }
        for (int y = access.minHeight(); y < access.maxHeight(); y++) {
            for (int i = 0; i < 16; i++) {
                sourceIfLit(access, into, minX - 1, y, minZ + i, minX, y, minZ + i);
                sourceIfLit(access, into, minX + 16, y, minZ + i, minX + 15, y, minZ + i);
                sourceIfLit(access, into, minX + i, y, minZ - 1, minX + i, y, minZ);
                sourceIfLit(access, into, minX + i, y, minZ + 16, minX + i, y, minZ + 15);
            }
        }
    }

    private static boolean needsSpread(LightAccess access, LightPropagation into, int x, int y, int z, int level) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }
            long neighbour = LightPositions.offset(LightPositions.pack(x, y, z), direction);
            int nx = LightPositions.x(neighbour);
            int ny = LightPositions.y(neighbour);
            int nz = LightPositions.z(neighbour);
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                continue;
            }
            if (access.light(nx, ny, nz) < into.arrive(access, x, y, z, nx, ny, nz, direction, level)) {
                return true;
            }
        }
        return false;
    }

    private static void sourceIfLit(LightAccess access, LightPropagation into,
            int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (!access.isLoaded(fromX, fromZ)) {
            return;
        }
        int level = access.light(fromX, fromY, fromZ);
        if (level <= 0) {
            return;
        }
        Direction direction = directionTo(fromX, fromZ, toX, toZ);
        if (into.arrive(access, fromX, fromY, fromZ, toX, toY, toZ, direction, level) > access.light(toX, toY, toZ)) {
            into.source(LightPositions.pack(fromX, fromY, fromZ));
        }
    }

    private static Direction directionTo(int fromX, int fromZ, int toX, int toZ) {
        if (toX > fromX) {
            return Direction.EAST;
        }
        if (toX < fromX) {
            return Direction.WEST;
        }
        return toZ > fromZ ? Direction.SOUTH : Direction.NORTH;
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
