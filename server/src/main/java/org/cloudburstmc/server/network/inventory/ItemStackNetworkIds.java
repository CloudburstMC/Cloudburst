package org.cloudburstmc.server.network.inventory;

import com.google.common.collect.MapMaker;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemStack;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns Bedrock network IDs to live stack instances and server-created offers.
 */
@UtilityClass
public class ItemStackNetworkIds {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final ConcurrentMap<ItemStack, Integer> STACK_IDS = new MapMaker().weakKeys().makeMap();

    public static int get(ItemStack stack) {
        return stack.isEmpty() ? 0 : STACK_IDS.computeIfAbsent(stack, ignored -> allocate());
    }

    public static int allocate() {
        return NEXT_ID.getAndUpdate(id -> id == Integer.MAX_VALUE ? 1 : id + 1);
    }
}
