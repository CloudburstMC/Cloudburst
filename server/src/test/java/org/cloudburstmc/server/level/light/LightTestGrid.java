package org.cloudburstmc.server.level.light;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutable {@link LightAccess} over a bounded box for engine tests.
 *
 * <p>Writes outside the loaded box or height fail; reads outside return air
 * or 0 like production.
 */
final class LightTestGrid implements LightAccess {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final LightStateCache STATES = new LightStateCache(REGISTRY);

    final Map<Vector3i, BlockState> blocks = new HashMap<>();
    final Map<Vector3i, BlockState> secondary = new HashMap<>();
    final Map<Vector3i, Integer> lights = new HashMap<>();
    int minX;
    int maxX;
    int minZ;
    int maxZ;
    private final int minY;
    private final int maxY;

    LightTestGrid(int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public int minHeight() {
        return this.minY;
    }

    @Override
    public int maxHeight() {
        return this.maxY;
    }

    @Override
    public boolean isLoaded(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    @Override
    public BlockState primary(int x, int y, int z) {
        if (!this.isLoaded(x, z) || y < this.minY || y >= this.maxY) {
            return BlockStates.AIR;
        }
        return this.blocks.getOrDefault(Vector3i.from(x, y, z), BlockStates.AIR);
    }

    @Override
    public BlockState secondary(int x, int y, int z) {
        if (!this.isLoaded(x, z) || y < this.minY || y >= this.maxY) {
            return BlockStates.AIR;
        }
        return this.secondary.getOrDefault(Vector3i.from(x, y, z), BlockStates.AIR);
    }

    @Override
    public int light(int x, int y, int z) {
        if (!this.isLoaded(x, z) || y < this.minY || y >= this.maxY) {
            return 0;
        }
        return this.lights.getOrDefault(Vector3i.from(x, y, z), 0);
    }

    @Override
    public void setLight(int x, int y, int z, int light) {
        Preconditions.checkArgument(this.isLoaded(x, z) && y >= this.minY && y < this.maxY,
                "position outside the loaded box: %s, %s, %s", x, y, z);
        this.lights.put(Vector3i.from(x, y, z), light);
    }

    @Override
    public LightStateCache states() {
        return STATES;
    }

    int lightAt(Vector3i position) {
        return this.light(position.getX(), position.getY(), position.getZ());
    }

    void initializeBlock(int chunkX, int chunkZ) {
        LightPropagation propagation = new LightPropagation(LightChannel.BLOCK);
        BlockLightEngine.initialize(this, chunkX, chunkZ, propagation);
        assertTrue(propagation.run(this, Integer.MAX_VALUE), "block propagation did not drain");
    }

    void updateBlock(Collection<Vector3i> changed) {
        LightPropagation propagation = new LightPropagation(LightChannel.BLOCK);
        for (Vector3i position : changed) {
            propagation.invalidate(this,
                    LightPositions.pack(position.getX(), position.getY(), position.getZ()));
        }
        assertTrue(propagation.run(this, Integer.MAX_VALUE), "block propagation did not drain");
    }

    void initializeSky(int chunkX, int chunkZ) {
        LightPropagation propagation = new LightPropagation(LightChannel.SKY);
        SkyLightEngine.initialize(this, chunkX, chunkZ, propagation);
        assertTrue(propagation.run(this, Integer.MAX_VALUE), "sky propagation did not drain");
    }

    void updateSky(Collection<Vector3i> changed) {
        LightPropagation propagation = new LightPropagation(LightChannel.SKY);
        for (Vector3i position : changed) {
            propagation.invalidate(this,
                    LightPositions.pack(position.getX(), position.getY(), position.getZ()));
        }
        assertTrue(propagation.run(this, Integer.MAX_VALUE), "sky propagation did not drain");
    }
}
