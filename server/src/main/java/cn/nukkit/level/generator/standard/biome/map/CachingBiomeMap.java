package cn.nukkit.level.generator.standard.biome.map;

import cn.nukkit.level.chunk.Chunk;
import cn.nukkit.level.generator.standard.biome.GenerationBiome;
import cn.nukkit.utils.Identifier;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import lombok.NonNull;
import net.daporkchop.lib.common.util.PValidation;

/**
 * Implementation of {@link BiomeMap} which caches the biomes looked up at a given position.
 *
 * @author DaPorkchop_
 */
public final class CachingBiomeMap implements BiomeMap {
    private final Long2ObjectLinkedOpenHashMap<GenerationBiome[]> cache;
    private final BiomeMap delegate;
    private final int maxCacheSize;

    public CachingBiomeMap(BiomeMap delegate) {
        this(delegate, Runtime.getRuntime().availableProcessors() * 256);
    }

    public CachingBiomeMap(@NonNull BiomeMap delegate, int maxCacheSize) {
        this.cache = new Long2ObjectLinkedOpenHashMap<>(PValidation.ensurePositive(maxCacheSize));
        this.delegate = delegate;
        this.maxCacheSize = maxCacheSize;
    }

    private GenerationBiome[] getChunk(int chunkX, int chunkZ) {
        long key = Chunk.key(chunkX, chunkZ);
        GenerationBiome[] chunk;
        boolean generate = false;
        synchronized (this.cache) {
            if ((chunk = this.cache.getAndMoveToLast(key)) == null) {
                //chunk doesn't exist
                if (this.cache.size() >= this.maxCacheSize) {
                    this.cache.removeFirst();
                }
                this.cache.put(key, chunk = new GenerationBiome[16 * 16]);
                generate = true;
            }
        }

        if (generate) {
            //we need to generate the actual biome data to fill the array with
            synchronized (chunk) {
                Preconditions.checkState(this.delegate.getRegion(chunk, chunkX << 4, chunkZ << 4, 16, 16) == chunk);
                Preconditions.checkState(chunk[255] != null);
            }
            return chunk;
        }

        do {
            //wait for chunk to be generated by another thread if not already done
            synchronized (chunk) {
                if (chunk[255] != null) {
                    return chunk;
                }
            }
        } while (true);
    }

    @Override
    public GenerationBiome get(int x, int z) {
        return this.getChunk(x >> 4, z >> 4)[((x & 0xF) << 4) | (z & 0xF)];
    }

    @Override
    public GenerationBiome[] getRegion(GenerationBiome[] arr, int x, int z, int sizeX, int sizeZ) {
        int totalSize = PValidation.ensurePositive(sizeX) * PValidation.ensurePositive(sizeZ);
        if (arr == null || arr.length < totalSize) {
            arr = new GenerationBiome[totalSize];
        }

        if ((x & 0xF) == 0 && (z & 0xF) == 0 && sizeX == 16 && sizeZ == 16) {
            //fast-track for fetching a single chunk at a time
            System.arraycopy(this.getChunk(x >> 4, z >> 4), 0, arr, 0, 16 * 16);
            return arr;
        }

        //this could be optimized some more, but it's unlikely it'll be called frequently if at all
        for (int dx = 0; dx < sizeX; dx++)   {
            for (int dz = 0; dz < sizeZ; dz++)  {
                arr[dx * sizeZ + dz] = this.getChunk((x + dx) >> 4, (z + dz) >> 4)[(((x + dx) & 0xF) << 4) | ((z + dz) & 0xF)];
            }
        }

        return arr;
    }

    @Override
    public Identifier[] getRegionIds(Identifier[] arr, int x, int z, int sizeX, int sizeZ) {
        int totalSize = PValidation.ensurePositive(sizeX) * PValidation.ensurePositive(sizeZ);
        if (arr == null || arr.length < totalSize) {
            arr = new Identifier[totalSize];
        }

        if ((x & 0xF) == 0 && (z & 0xF) == 0 && sizeX == 16 && sizeZ == 16) {
            //fast-track for fetching a single chunk at a time
            GenerationBiome[] chunk = this.getChunk(x >> 4, z >> 4);
            for (int i = 0; i < 16 * 16; i++) {
                arr[i] = chunk[i].getId();
            }
            return arr;
        }

        for (int dx = 0; dx < sizeX; dx++)   {
            for (int dz = 0; dz < sizeZ; dz++)  {
                arr[dx * sizeZ + dz] = this.getChunk((x + dx) >> 4, (z + dz) >> 4)[(((x + dx) & 0xF) << 4) | ((z + dz) & 0xF)].getId();
            }
        }

        return arr;
    }

    @Override
    public boolean needsCaching() {
        //this is already cached, no reason to cache it a second time :P
        return false;
    }

    @Override
    public Identifier getId() {
        //transparently forward along to delegate
        return this.delegate.getId();
    }
}
