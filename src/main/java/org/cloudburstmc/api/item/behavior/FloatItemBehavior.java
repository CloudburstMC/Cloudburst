package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface FloatItemBehavior {

    float get(Behavior<Executor> behavior, ItemStack itemStack);

    @FunctionalInterface
    interface Executor {

        float execute(ItemStack itemStack);
    }
}
