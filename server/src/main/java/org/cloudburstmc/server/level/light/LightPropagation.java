package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.Direction;

public final class LightPropagation {

    private final LightChannel channel;
    private final LongArrayFIFOQueue decreasePositions = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue decreaseLevels = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue increasePositions = new LongArrayFIFOQueue();

    public LightPropagation(LightChannel channel) {
        this.channel = channel;
    }

    public void invalidate(LightAccess access, long pos) {
        int x = LightPositions.x(pos);
        int y = LightPositions.y(pos);
        int z = LightPositions.z(pos);
        int old = access.light(x, y, z);
        int own = ownValue(access, x, y, z);
        if (own != old) {
            access.setLight(x, y, z, own);
        }
        this.decreasePositions.enqueue(pos);
        this.decreaseLevels.enqueue(old);
        for (Direction direction : Direction.values()) {
            long neighbour = LightPositions.offset(pos, direction);
            int nx = LightPositions.x(neighbour);
            int ny = LightPositions.y(neighbour);
            int nz = LightPositions.z(neighbour);
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                continue;
            }
            int current = access.light(nx, ny, nz);
            int relaxed = recompute(access, nx, ny, nz);
            if (relaxed < current) {
                access.setLight(nx, ny, nz, relaxed);
                this.decreasePositions.enqueue(neighbour);
                this.decreaseLevels.enqueue(current);
            }
            this.increasePositions.enqueue(neighbour);
        }
        if (own > 0) {
            this.increasePositions.enqueue(pos);
        }
    }

    public void source(long pos) {
        this.increasePositions.enqueue(pos);
    }

    public boolean run(LightAccess access, int maxSteps) {
        int steps = 0;
        while (steps < maxSteps && (!this.decreasePositions.isEmpty() || !this.increasePositions.isEmpty())) {
            if (!this.decreasePositions.isEmpty()) {
                long position = this.decreasePositions.dequeueLong();
                int oldLevel = (int) this.decreaseLevels.dequeueLong();
                decrease(access, position, oldLevel);
            } else {
                increase(access, this.increasePositions.dequeueLong());
            }
            steps++;
        }
        return this.decreasePositions.isEmpty() && this.increasePositions.isEmpty();
    }

    private void decrease(LightAccess access, long position, int oldLevel) {
        int x = LightPositions.x(position);
        int y = LightPositions.y(position);
        int z = LightPositions.z(position);
        for (Direction direction : Direction.values()) {
            long neighbour = LightPositions.offset(position, direction);
            int nx = LightPositions.x(neighbour);
            int ny = LightPositions.y(neighbour);
            int nz = LightPositions.z(neighbour);
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                continue;
            }
            int current = access.light(nx, ny, nz);
            if (current <= 0) {
                continue;
            }
            if (current <= arrive(access, x, y, z, nx, ny, nz, direction, oldLevel)) {
                int own = ownValue(access, nx, ny, nz);
                if (own != current) {
                    access.setLight(nx, ny, nz, own);
                    this.decreasePositions.enqueue(neighbour);
                    this.decreaseLevels.enqueue(current);
                }
                if (own > 0) {
                    this.increasePositions.enqueue(neighbour);
                }
            } else {
                this.increasePositions.enqueue(neighbour);
            }
        }
    }

    private void increase(LightAccess access, long position) {
        int x = LightPositions.x(position);
        int y = LightPositions.y(position);
        int z = LightPositions.z(position);
        if (y < access.minHeight() || y >= access.maxHeight() || !access.isLoaded(x, z)) {
            return;
        }
        int level = access.light(x, y, z);
        if (level <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            long neighbour = LightPositions.offset(position, direction);
            int nx = LightPositions.x(neighbour);
            int ny = LightPositions.y(neighbour);
            int nz = LightPositions.z(neighbour);
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                continue;
            }
            int arrival = arrive(access, x, y, z, nx, ny, nz, direction, level);
            if (arrival > access.light(nx, ny, nz)) {
                access.setLight(nx, ny, nz, arrival);
                this.increasePositions.enqueue(neighbour);
            }
        }
    }

    int arrive(LightAccess access, int fromX, int fromY, int fromZ, int toX, int toY, int toZ,
            Direction direction, int level) {
        LightStateCache states = access.states();
        BlockState fromPrimary = access.primary(fromX, fromY, fromZ);
        BlockState fromSecondary = access.secondary(fromX, fromY, fromZ);
        BlockState toPrimary = access.primary(toX, toY, toZ);
        BlockState toSecondary = access.secondary(toX, toY, toZ);
        if (states.occludes(fromPrimary, fromSecondary, toPrimary, toSecondary, direction)) {
            return 0;
        }
        if (this.channel == LightChannel.BLOCK) {
            if (states.opacity(toPrimary, toSecondary) >= 15 || states.emission(toPrimary, toSecondary) >= 15) {
                return 0;
            }
            return level - Math.max(1, states.opacity(toPrimary, toSecondary));
        }
        int opacity = states.opacity(toPrimary, toSecondary);
        if (opacity >= 15) {
            return 0;
        }
        if (direction == Direction.DOWN && level == 15) {
            return 15 - opacity;
        }
        return level - Math.max(1, opacity);
    }

    private int ownValue(LightAccess access, int x, int y, int z) {
        if (this.channel == LightChannel.BLOCK) {
            return access.states().emission(access.primary(x, y, z), access.secondary(x, y, z));
        }
        if (y == access.maxHeight() - 1
                && !access.states().occludes(BlockStates.AIR, BlockStates.AIR,
                        access.primary(x, y, z), access.secondary(x, y, z), Direction.DOWN)) {
            return Math.max(0, 15 - access.states().opacity(access.primary(x, y, z), access.secondary(x, y, z)));
        }
        return 0;
    }

    private int recompute(LightAccess access, int x, int y, int z) {
        LightStateCache states = access.states();
        BlockState primary = access.primary(x, y, z);
        BlockState secondary = access.secondary(x, y, z);
        if (this.channel == LightChannel.BLOCK) {
            int light = states.emission(primary, secondary);
            int attenuation = Math.max(1, states.opacity(primary, secondary));
            if (light < 15 && attenuation < 15) {
                for (Direction direction : Direction.values()) {
                    long neighbour = LightPositions.offset(LightPositions.pack(x, y, z), direction);
                    int nx = LightPositions.x(neighbour);
                    int ny = LightPositions.y(neighbour);
                    int nz = LightPositions.z(neighbour);
                    if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                        continue;
                    }
                    int incoming = access.light(nx, ny, nz);
                    if (incoming - attenuation > light && !states.occludes(access.primary(nx, ny, nz),
                            access.secondary(nx, ny, nz), primary, secondary, direction.getOpposite())) {
                        light = incoming - attenuation;
                    }
                }
            }
            return Math.clamp(light, 0, 15);
        }
        int opacity = states.opacity(primary, secondary);
        int light = 0;
        if (opacity < 15) {
            for (Direction direction : Direction.values()) {
                long neighbour = LightPositions.offset(LightPositions.pack(x, y, z), direction);
                int nx = LightPositions.x(neighbour);
                int ny = LightPositions.y(neighbour);
                int nz = LightPositions.z(neighbour);
                BlockState fromPrimary;
                BlockState fromSecondary;
                int incoming;
                if (direction == Direction.UP && ny == access.maxHeight()) {
                    fromPrimary = BlockStates.AIR;
                    fromSecondary = BlockStates.AIR;
                    incoming = 15;
                } else {
                    if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                        continue;
                    }
                    fromPrimary = access.primary(nx, ny, nz);
                    fromSecondary = access.secondary(nx, ny, nz);
                    incoming = access.light(nx, ny, nz);
                }
                int attenuation = direction == Direction.UP && incoming == 15 ? opacity : Math.max(1, opacity);
                if (incoming - attenuation > light && !states.occludes(fromPrimary, fromSecondary,
                        primary, secondary, direction.getOpposite())) {
                    light = incoming - attenuation;
                }
            }
        }
        return Math.clamp(light, 0, 15);
    }
}
