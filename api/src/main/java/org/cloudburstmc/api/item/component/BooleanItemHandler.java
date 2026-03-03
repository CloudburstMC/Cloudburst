package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface BooleanItemHandler {

    boolean execute(ItemStack itemStack);
}
