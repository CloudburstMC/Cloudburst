package org.cloudburstmc.server.network.inventory;

import com.google.common.collect.MapMaker;
import org.cloudburstmc.api.item.ItemStack;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns stack and offer IDs within one player connection.
 */
public class ItemStackNetworkIds {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentMap<ItemStack, Integer> stackIds = new MapMaker().weakKeys().makeMap();

    public int get(ItemStack stack) {
        return stack.isEmpty() ? 0 : this.stackIds.computeIfAbsent(stack, ignored -> this.allocate());
    }

    public boolean matches(ItemStack stack, int id) {
        if (stack.isEmpty()) {
            return id == 0;
        }

        Integer assigned = this.stackIds.get(stack);
        return assigned != null && assigned == id;
    }

    public int allocate() {
        return this.nextId.getAndUpdate(id -> id == Integer.MAX_VALUE ? 1 : id + 1);
    }
}
