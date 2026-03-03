package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface IntItemHandler {

    int execute(ItemStack itemStack);
}
