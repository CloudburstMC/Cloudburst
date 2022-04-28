package org.cloudburstmc.server.item;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.RegistryException;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class NetworkItemManager {

    //StackNetId stuff
    private final Int2ReferenceMap<WeakReference<ItemStack>> netIdMap = new Int2ReferenceOpenHashMap<>();
    private final ReferenceQueue<ItemStack> oldIdQueue = new ReferenceQueue<>();

    public NetworkItemManager() {
        this.netIdMap.put(0, new WeakReference<>(ItemStack.AIR, oldIdQueue));
    }

    public int getNextNetId() {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            if (!netIdMap.containsKey(i) || netIdMap.get(i).refersTo(null)) {
                return i;
            }
        }
        return -1;
    }

    public void addNetId(@Nonnull ItemStack item) {
        if (item.getStackNetworkId() == -1) {
            throw new RegistryException("Invalid network stack id for item: " + item);
        }
        if (item.getStackNetworkId() == 0 || item.getType() == BlockTypes.AIR) return;
        WeakReference<ItemStack> ref = new WeakReference<>(item, oldIdQueue);
        netIdMap.put(item.getStackNetworkId(), ref);
    }

    @Nonnull
    public ItemStack getItemByNetId(@Nonnull int stackNetId) {
        WeakReference<ItemStack> ref = netIdMap.getOrDefault(stackNetId, null);
        if (ref == null || ref.refersTo(null)) return ItemStack.AIR;
        return Objects.requireNonNull(ref.get());

    }

    public void clearQueue() { //TODO - call this how often? Every server tick, every x minutes on a new thread? need to syncronize?
        java.lang.ref.Reference<? extends ItemStack> ref;
        while ((ref = oldIdQueue.poll()) != null) {
            if (ref.get() != null && ref.get() != ItemStack.AIR)
                netIdMap.remove(ref.get().getStackNetworkId());
        }
    }
}
