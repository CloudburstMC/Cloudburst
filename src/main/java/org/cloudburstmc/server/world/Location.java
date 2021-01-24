package org.cloudburstmc.server.world;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.world.chunk.Chunk;

import static com.google.common.base.Preconditions.checkNotNull;

@EqualsAndHashCode
public final class Location {
    private final Vector3f position;
    private final float yaw;
    private final float pitch;
    private final World world;

    private Location(Vector3f position, float yaw, float pitch, World world) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    public static Location from(World world) {
        return from(Vector3f.ZERO, world);
    }

    public static Location from(Vector3i position, World world) {
        return from(position.getX(), position.getY(), position.getZ(), 0f, 0f, world);
    }

    public static Location from(float x, float y, float z, World world) {
        return from(x, y, z, 0, 0, world);
    }

    public static Location from(Vector3f position, World world) {
        return from(position, 0f, 0f, world);
    }

    public static Location from(Vector3f position, float yaw, float pitch, World world) {
        checkNotNull(position, "position");
        checkNotNull(world, "world");
        return new Location(position, yaw, pitch, world);
    }

    public static Location from(float x, float y, float z, float yaw, float pitch, World world) {
        checkNotNull(world, "world");
        return new Location(Vector3f.from(x, y, z), yaw, pitch, world);
    }

    public float getX() {
        return this.position.getX();
    }

    public float getY() {
        return this.position.getY();
    }

    public float getZ() {
        return this.position.getZ();
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public World getWorld() {
        return world;
    }

    public Chunk getChunk() {
        return world.getChunk(this.getChunkX(), this.getChunkZ());
    }

    public Block getBlock() {
        return world.getBlock(this.position);
    }

    public Location add(double x, double y, double z) {
        return Location.from(Vector3f.from(this.getX() + x, this.getY() + y, this.getZ() + z), this.yaw, this.pitch, this.world);
    }

    public Location add(float x, float y, float z) {
        return Location.from(Vector3f.from(this.getX() + x, this.getY() + y, this.getZ() + z), this.yaw, this.pitch, this.world);
    }

    public int getFloorX() {
        return this.position.getFloorX();
    }

    public int getFloorY() {
        return this.position.getFloorY();
    }

    public int getFloorZ() {
        return this.position.getFloorZ();
    }

    public int getChunkX() {
        return this.getFloorX() >> 4;
    }

    public int getChunkZ() {
        return this.getFloorZ() >> 4;
    }
}
