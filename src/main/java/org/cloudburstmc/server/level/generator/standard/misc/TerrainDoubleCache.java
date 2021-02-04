package org.cloudburstmc.server.level.generator.standard.misc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PValidation;
import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public abstract class TerrainDoubleCache {
    protected static final long NaN = Double.doubleToRawLongBits(Double.NaN);

    protected final Ref<Long2LongLinkedOpenHashMap> cacheCache = ThreadRef.soft(Long2LongLinkedOpenHashMap::new);
    protected final int radius;
    protected final int scale;

    @JsonCreator
    public TerrainDoubleCache(
            @JsonProperty(value = "radius", required = true) int radius,
            @JsonProperty(value = "scale", required = true) int scale) {
        this.radius = (int) PValidation.notNegative(radius);
        this.scale = (int) PValidation.notNegative(scale);
    }

    public double get(int x, int z) {
        Long2LongLinkedOpenHashMap cache = this.cacheCache.get();
        long val = cache.getOrDefault(CloudChunk.key(x, z), NaN);
        if (val == NaN) {
            if (cache.size() >= 1024) {
                cache.removeFirstLong();
            }

            cache.put(CloudChunk.key(x, z), val = Double.doubleToRawLongBits(this.computeValue(x, z, this.radius, this.scale)));
        }
        return Double.longBitsToDouble(val);
    }

    protected abstract double computeValue(int x, int z, int radius, int scale);
}
