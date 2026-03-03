package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface GetItemHandler<T> {

    T execute(ItemStack itemStack);
}
