package org.cloudburstmc.server.network.inventory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.nbt.NbtMap;

import java.lang.ref.SoftReference;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkItemStack {

    private static final AtomicInteger NET_ID_ALLOCATOR = new AtomicInteger();

    private static final Cache<Integer, ItemStack> NET_ID_CACHE = CacheBuilder.newBuilder()
            .weakValues() // Make sure the entry is removed when the item is no longer referenced
            .build();

    private static final Cache<ItemStack, NetworkItemStack> ITEM_CACHE = CacheBuilder.newBuilder()
            .weakKeys() // Make sure the entry is removed when the item is no longer referenced
            .build();

    private final int netId = NET_ID_ALLOCATOR.getAndUpdate(operand -> ++operand <= 0 ? 1 : operand);
    private SoftReference<NbtMap> tag;

    private static NetworkItemStack get(ItemStack itemStack) {
        try {
            return ITEM_CACHE.get(itemStack, () -> {
                NetworkItemStack networkItemStack = new NetworkItemStack();
                NET_ID_CACHE.put(networkItemStack.netId, itemStack);
                return networkItemStack;
            });
        } catch (ExecutionException e) {
            throw new AssertionError("Unexpected exception", e);
        }
    }

    public static int getNetId(ItemStack itemStack) {
        return get(itemStack).netId;
    }

    public static Optional<NbtMap> getCachedNbt(ItemStack itemStack) {
        SoftReference<NbtMap> reference = get(itemStack).tag;
        return reference == null ? Optional.empty() : Optional.ofNullable(reference.get());
    }

    public static void setCachedNbt(ItemStack itemStack, NbtMap tag) {
        get(itemStack).tag = new SoftReference<>(tag);
    }

    public static ItemStack getItemStack(int netId) {
        return NET_ID_CACHE.getIfPresent(netId);
    }
}
