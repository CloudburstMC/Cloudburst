package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface DamageItemBehavior {

    ItemStack onDamage(Behavior<Executor> behavior, ItemStack itemStack, int damage, Entity owner);

    interface Executor {

        ItemStack execute(ItemStack itemStack, int damage, Entity owner);
    }
}
