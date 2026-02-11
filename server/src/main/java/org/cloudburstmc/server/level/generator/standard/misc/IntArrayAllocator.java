package org.cloudburstmc.server.level.generator.standard.misc;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.common.util.PValidation;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A simple pooling allocator for {@code int[]}s.
 * <p>
 * Not thread-safe!
 *
 * @author DaPorkchop_
 */
public class IntArrayAllocator {
    public static final Cached<IntArrayAllocator> DEFAULT = Cached.threadLocal(() -> new IntArrayAllocator(8), ReferenceStrength.SOFT);

    protected final Deque<int[]>[] arenas;
    protected final int maxArenaSize;

    public IntArrayAllocator(int maxArenaSize) {
        this.maxArenaSize = PValidation.positive(maxArenaSize, "maxArenaSize");
        this.arenas = PorkUtil.uncheckedCast(new Deque[32]);
        for (int i = 0; i < 32; i++) {
            this.arenas[i] = new ArrayDeque<>(maxArenaSize);
        }
    }

    public int[] get(int minSize) {
        Preconditions.checkArgument(minSize > 0);

        int minRequiredBits = 32 - Integer.numberOfLeadingZeros(minSize - 1);
        int[] arr = this.arenas[minRequiredBits].pollLast();
        return arr != null ? arr : new int[1 << minRequiredBits];
    }

    public void release(@NonNull int[] arr) {
        int length = arr.length;
        Preconditions.checkArgument(length != 0 && BinMath.isPow2(length));

        int minRequiredBits = 32 - Integer.numberOfLeadingZeros(length - 1);
        Deque<int[]> arena = this.arenas[minRequiredBits];
        if (arena.size() < this.maxArenaSize) {
            arena.addLast(arr);
        }
    }
}
