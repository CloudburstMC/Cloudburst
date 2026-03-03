package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface FloatItemHandler {

    float execute(ItemStack itemStack);
}
