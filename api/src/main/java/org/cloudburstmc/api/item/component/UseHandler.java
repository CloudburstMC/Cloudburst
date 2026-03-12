package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface UseHandler {

    ItemStack execute(ItemStack itemStack, Entity entity);
}
