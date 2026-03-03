package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface DamageItemHandler {

    ItemStack execute(ItemStack itemStack, int damage, Entity owner);
}
