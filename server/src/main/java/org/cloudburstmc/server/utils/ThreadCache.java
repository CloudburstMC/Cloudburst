package org.cloudburstmc.server.utils;

public class ThreadCache {

    public static final ThreadLocal<int[]> intCache256 = ThreadLocal.withInitial(() -> new int[256]);

    /**
     * Removes the cached buffer from the calling thread's ThreadLocalMap,
     * allowing the GC to reclaim it. The buffer is re-created lazily on
     * the next {@code intCache256.get()} call from that thread.
     */
    public static void clean() {
        intCache256.remove();
    }
}
