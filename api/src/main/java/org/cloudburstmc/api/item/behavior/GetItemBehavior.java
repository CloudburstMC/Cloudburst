package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface GetItemBehavior<T> {

    T get(Behavior<Executor<T>> behavior, ItemStack item);

    @FunctionalInterface
    interface Executor<T> {

        T execute(ItemStack itemStack);
    }
}
